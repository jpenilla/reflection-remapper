/*
 * reflection-remapper
 *
 * Copyright (c) 2021 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jpenilla.reflectionremapper.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.internal.util.Util;
import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

final class ReflectionProxyInvocationHandler<I> implements InvocationHandler {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};
  private final Class<I> interfaceClass;
  private final Class<?> proxiedClass;
  private final Map<Method, MethodHandle> methodHandles = new ConcurrentHashMap<>();

  ReflectionProxyInvocationHandler(
    final Class<I> interfaceClass,
    final Class<?> proxiedClass,
    final ReflectionRemapper reflectionRemapper
  ) {
    this.interfaceClass = interfaceClass;
    this.proxiedClass = proxiedClass;
    this.scanInterface(
      reflectionRemapper::remapClassName,
      fieldName -> reflectionRemapper.remapFieldName(this.proxiedClass, fieldName),
      (methodName, parameters) -> reflectionRemapper.remapMethodName(this.proxiedClass, methodName, parameters)
    );
  }

  @Override
  public @Nullable Object invoke(
    final Object proxy,
    final Method method,
    Object[] args
  ) throws Throwable {
    if (isEqualsMethod(method)) {
      return proxy == args[0];
    } else if (isHashCodeMethod(method)) {
      return 0;
    } else if (isToStringMethod(method)) {
      return "ReflectionProxy[interface=%s, implementation=%s, proxies=%s]"
        .formatted(this.interfaceClass.getTypeName(), proxy.getClass().getTypeName(), this.proxiedClass.getTypeName());
    }

    if (args == null) {
      args = EMPTY_OBJECT_ARRAY;
    }

    if (method.isDefault()) {
      return this.handleDefaultMethod(proxy, method, args);
    }

    final boolean hasStaticAnnotation = method.getDeclaredAnnotation(Static.class) != null;

    final @Nullable FieldGetter getterAnnotation = method.getDeclaredAnnotation(FieldGetter.class);
    if (getterAnnotation != null) {
      if (hasStaticAnnotation) {
        return this.methodHandles.get(method).invoke();
      }
      return this.methodHandles.get(method).bindTo(args[0]).invoke();
    }

    final @Nullable FieldSetter setterAnnotation = method.getDeclaredAnnotation(FieldSetter.class);
    if (setterAnnotation != null) {
      if (hasStaticAnnotation) {
        return this.methodHandles.get(method).invokeWithArguments(args[0]);
      }
      return this.methodHandles.get(method).bindTo(args[0]).invokeWithArguments(args[1]);
    }

    if (hasStaticAnnotation) {
      return this.methodHandles.get(method).invokeWithArguments(args);
    }
    return this.methodHandles.get(method).bindTo(args[0]).invokeWithArguments(Arrays.stream(args).skip(1).toArray());
  }

  private @Nullable Object handleDefaultMethod(
    final Object proxy,
    final Method method,
    final Object[] args
  ) throws Throwable {
    final MethodHandle handle = this.methodHandles.computeIfAbsent(
      method,
      m -> Util.sneakyThrows(
        () -> MethodHandles.privateLookupIn(proxy.getClass(), LOOKUP)
          .findSpecial(
            this.interfaceClass,
            m.getName(),
            MethodType.methodType(m.getReturnType(), m.getParameterTypes()),
            this.interfaceClass
          )
          .bindTo(proxy)
      )
    );

    if (args.length == 0) {
      return handle.invokeExact();
    } else {
      return handle.invokeWithArguments(args);
    }
  }

  private void scanInterface(
    final UnaryOperator<String> classMapper,
    final UnaryOperator<String> fieldMapper,
    final BiFunction<String, Class<?>[], String> methodMapper
  ) {
    for (final Method method : this.interfaceClass.getDeclaredMethods()) {
      if (isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method)) {
        continue;
      } else if (method.isDefault()) {
        // We just load default methods lazily, no mappings need to be resolved so there is no need to eagerly evaluate them before mappings are discarded.
        continue;
      }

      final boolean constructorInvoker = method.getDeclaredAnnotation(ConstructorInvoker.class) != null;
      if (constructorInvoker) {
        this.methodHandles.put(
          method,
          Util.sneakyThrows(() -> LOOKUP.unreflectConstructor(this.findProxiedConstructor(method, classMapper)))
        );
        continue;
      }

      final @Nullable FieldGetter getterAnnotation = method.getDeclaredAnnotation(FieldGetter.class);
      final @Nullable FieldSetter setterAnnotation = method.getDeclaredAnnotation(FieldSetter.class);
      if (getterAnnotation != null && setterAnnotation != null) {
        throw new IllegalArgumentException("Method " + method.getName() + " in " + this.interfaceClass.getTypeName() + " is annotated with @FieldGetter and @FieldSetter, don't know which to use.");
      }

      final boolean hasStaticAnnotation = method.getDeclaredAnnotation(Static.class) != null;

      if (getterAnnotation != null) {
        if (hasStaticAnnotation) {
          checkParameterCount(method, this.interfaceClass, 0, "Static @FieldGetters should have no parameters.");
        } else {
          checkParameterCount(method, this.interfaceClass, 1, "Non-static @FieldGetters should have one parameter.");
        }

        this.methodHandles.put(
          method,
          Util.sneakyThrows(() -> LOOKUP.unreflectGetter(getDeclaredFieldAndSetAccessible(this.proxiedClass, getterAnnotation.value(), fieldMapper)))
        );
        continue;
      }

      if (setterAnnotation != null) {
        if (hasStaticAnnotation) {
          checkParameterCount(method, this.interfaceClass, 1, "Static @FieldSetters should have one parameter.");
        } else {
          checkParameterCount(method, this.interfaceClass, 2, "Non-static @FieldSetters should have two parameters.");
        }

        this.methodHandles.put(
          method,
          Util.sneakyThrows(() -> LOOKUP.unreflectSetter(getDeclaredFieldAndSetAccessible(this.proxiedClass, setterAnnotation.value(), fieldMapper)))
        );
        continue;
      }

      if (!hasStaticAnnotation && method.getParameterCount() < 1) {
        throw new IllegalArgumentException("Non-static method invokers should have at least one parameter. Method " + method.getName() + " in " + this.interfaceClass.getTypeName() + " has " + method.getParameterCount());
      }

      this.methodHandles.put(
        method,
        Util.sneakyThrows(() -> LOOKUP.unreflect(this.findProxiedMethod(method, classMapper, methodMapper)))
      );
    }
  }

  private static void checkParameterCount(final Method method, final Class<?> holder, final int expected, final String message) {
    if (method.getParameterCount() != expected) {
      throw new IllegalArgumentException(
        "Unexpected amount of parameters for method %s in %s, got %d while expecting %d. %s"
          .formatted(method.getName(), holder.getTypeName(), method.getParameterCount(), expected, message)
      );
    }
  }

  private static boolean isToStringMethod(final Method method) {
    return method.getName().equals("toString")
      && method.getParameterCount() == 0
      && method.getReturnType() == String.class;
  }

  private static boolean isHashCodeMethod(final Method method) {
    return method.getName().equals("hashCode")
      && method.getParameterCount() == 0
      && method.getReturnType() == int.class;
  }

  private static boolean isEqualsMethod(final Method method) {
    return method.getName().equals("equals")
      && method.getParameterCount() == 1
      && method.getReturnType() == boolean.class;
  }

  @SuppressWarnings("checkstyle:MethodName")
  private static Field getDeclaredFieldAndSetAccessible(
    final Class<?> holder,
    final String fieldName,
    final UnaryOperator<String> fieldMapper
  ) {
    final Field field;
    try {
      field = holder.getDeclaredField(fieldMapper.apply(fieldName));
    } catch (final NoSuchFieldException e) {
      throw new IllegalArgumentException("Could not find field '" + fieldName + "' in " + holder.getTypeName(), e);
    }
    if (!field.trySetAccessible()) {
      throw new IllegalStateException("Could not set access for field '" + fieldName + "' in " + holder.getTypeName());
    }
    return field;
  }

  private Constructor<?> findProxiedConstructor(
    final Method method,
    final UnaryOperator<String> classMapper
  ) {
    final Class<?>[] actualParams = Arrays.stream(method.getParameters())
      .map(p -> this.resolveParameterTypeClass(p, classMapper))
      .toArray(Class<?>[]::new);

    final Constructor<?> constructor;
    try {
      constructor = this.proxiedClass.getDeclaredConstructor(actualParams);
    } catch (final NoSuchMethodException ex) {
      throw new IllegalArgumentException("Could not find constructor of " + this.proxiedClass.getTypeName() + " with parameter types " + Arrays.toString(method.getParameterTypes()), ex);
    }
    if (!constructor.trySetAccessible()) {
      throw new IllegalStateException("Could not set access for proxy method target constructor of " + this.proxiedClass.getTypeName() + " with parameter types " + Arrays.toString(method.getParameterTypes()));
    }
    return constructor;
  }

  private Method findProxiedMethod(
    final Method method,
    final UnaryOperator<String> classMapper,
    final BiFunction<String, Class<?>[], String> methodMapper
  ) {
    final boolean hasStaticAnnotation = method.getDeclaredAnnotation(Static.class) != null;

    final Class<?>[] actualParams;
    if (hasStaticAnnotation) {
      actualParams = Arrays.stream(method.getParameters())
        .map(p -> this.resolveParameterTypeClass(p, classMapper))
        .toArray(Class<?>[]::new);
    } else {
      actualParams = Arrays.stream(method.getParameters())
        .skip(1)
        .map(p -> this.resolveParameterTypeClass(p, classMapper))
        .toArray(Class<?>[]::new);
    }

    final @Nullable MethodName methodAnnotation = method.getDeclaredAnnotation(MethodName.class);
    final String methodName = methodAnnotation == null ? method.getName() : methodAnnotation.value();
    final Method proxiedMethod;
    try {
      proxiedMethod = this.proxiedClass.getDeclaredMethod(methodMapper.apply(methodName, actualParams), actualParams);
    } catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Could not find proxy method target method: " + this.proxiedClass.getTypeName() + " " + methodName);
    }
    if (!proxiedMethod.trySetAccessible()) {
      throw new IllegalStateException("Could not set access for proxy method target method: " + this.proxiedClass.getTypeName() + " " + methodName);
    }

    return proxiedMethod;
  }

  private Class<?> resolveParameterTypeClass(
    final Parameter parameter,
    final UnaryOperator<String> classMapper
  ) {
    final @Nullable Type typeAnnotation = parameter.getDeclaredAnnotation(Type.class);
    if (typeAnnotation == null) {
      return parameter.getType();
    }

    if (typeAnnotation.value() == Object.class && typeAnnotation.className().isEmpty()) {
      throw new IllegalArgumentException("@Type annotation must either have value() or className() set.");
    }

    if (typeAnnotation.value() != Object.class) {
      return Util.findProxiedClass(typeAnnotation.value(), classMapper);
    }

    final Class<?> namedClass;
    try {
      namedClass = Class.forName(classMapper.apply(typeAnnotation.className()));
    } catch (final ClassNotFoundException e) {
      throw new IllegalArgumentException("Class " + typeAnnotation.className() + " specified in @Type annotation not found.", e);
    }

    return namedClass;
  }
}
