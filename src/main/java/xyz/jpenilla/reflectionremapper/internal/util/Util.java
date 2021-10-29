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
package xyz.jpenilla.reflectionremapper.internal.util;

import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@DefaultQualifier(NonNull.class)
public final class Util {
  private Util() {
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
  public static <E extends Throwable> void sneakyThrow(final Throwable ex) throws E {
    throw (E) ex;
  }

  public static <T> T sneakyThrows(final ThrowingSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (final Throwable ex) {
      sneakyThrow(ex);
      return null; // unreachable
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Throwable;
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
}
