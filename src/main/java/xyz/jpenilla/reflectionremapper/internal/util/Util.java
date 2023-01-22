/*
 * reflection-remapper
 *
 * Copyright (c) 2021-2023 Jason Penilla
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
package xyz.jpenilla.reflectionremapper.internal.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@DefaultQualifier(NonNull.class)
public final class Util {
  private Util() {
  }

  private static final @Nullable Method PRIVATE_LOOKUP_IN = findMethod(MethodHandles.class, "privateLookupIn", Class.class, MethodHandles.Lookup.class);
  private static final @Nullable Method DESCRIPTOR_STRING = findMethod(Class.class, "descriptorString");

  public static boolean mojangMapped() {
    return classExists("net.minecraft.server.level.ServerPlayer");
  }

  public static boolean classExists(final String className) {
    try {
      Class.forName(className);
      return true;
    } catch (final ClassNotFoundException ex) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Throwable> E sneakyThrow(final Throwable ex) throws E {
    throw (E) ex;
  }

  public static <T> T sneakyThrows(final ThrowingSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (final Throwable ex) {
      throw sneakyThrow(ex);
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Throwable;
  }

  public static boolean isSynthetic(final int modifiers) {
    return (modifiers & 0x1000 /* Opcodes.ACC_SYNTHETIC */) != 0;
  }

  public static Class<?> findProxiedClass(
    final Class<?> proxyInterface,
    final UnaryOperator<String> classMapper
  ) {
    if (!proxyInterface.isInterface()) {
      throw new IllegalArgumentException(proxyInterface.getTypeName() + " is not an interface annotated with @Proxies.");
    }

    final @Nullable Proxies proxies = proxyInterface.getDeclaredAnnotation(Proxies.class);
    if (proxies == null) {
      throw new IllegalArgumentException("interface " + proxyInterface.getTypeName() + " is not annotated with @Proxies.");
    }

    if (proxies.value() == Object.class && proxies.className().isEmpty()) {
      throw new IllegalArgumentException("@Proxies annotation must either have value() or className() set. Interface: " + proxyInterface.getTypeName());
    }

    if (proxies.value() != Object.class) {
      return proxies.value();
    }

    try {
      return Class.forName(classMapper.apply(proxies.className()));
    } catch (final ClassNotFoundException ex) {
      throw new IllegalArgumentException("Could not find class for @Proxied className() " + proxies.className() + ".");
    }
  }

  private static @Nullable Method findMethod(final Class<?> holder, final String name, final Class<?>... paramTypes) {
    try {
      return holder.getDeclaredMethod(name, paramTypes);
    } catch (final ReflectiveOperationException ex) {
      return null;
    }
  }

  public static MethodHandle handleForDefaultMethod(
    final Class<?> interfaceClass,
    final Method method
  ) throws Throwable {
    if (PRIVATE_LOOKUP_IN == null) { // jdk 8
      final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class);
      constructor.setAccessible(true);
      return constructor.newInstance(interfaceClass)
        .in(interfaceClass)
        .unreflectSpecial(method, interfaceClass);
    }

    // jdk 9+
    return ((MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, interfaceClass, MethodHandles.lookup()))
      .findSpecial(
        interfaceClass,
        method.getName(),
        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
        interfaceClass
      );
  }

  public static String descriptorString(final Class<?> clazz) {
    if (DESCRIPTOR_STRING != null) {
      // jdk 12+
      try {
        return (String) DESCRIPTOR_STRING.invoke(clazz);
      } catch (final ReflectiveOperationException ex) {
        throw new RuntimeException("Failed to call Class#descriptorString", ex);
      }
    }

    if (clazz == long.class) {
      return "J";
    } else if (clazz == int.class) {
      return "I";
    } else if (clazz == char.class) {
      return "C";
    } else if (clazz == short.class) {
      return "S";
    } else if (clazz == byte.class) {
      return "B";
    } else if (clazz == double.class) {
      return "D";
    } else if (clazz == float.class) {
      return "F";
    } else if (clazz == boolean.class) {
      return "Z";
    } else if (clazz == void.class) {
      return "V";
    }

    if (clazz.isArray()) {
      return "[" + descriptorString(clazz.getComponentType());
    }

    return 'L' + clazz.getName().replace('.', '/') + ';';
  }
}
