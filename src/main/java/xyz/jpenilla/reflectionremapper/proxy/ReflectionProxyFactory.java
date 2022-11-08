/*
 * reflection-remapper
 *
 * Copyright (c) 2021-2022 Jason Penilla
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

import java.lang.reflect.Proxy;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.internal.util.Util;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

/**
 * Factory for reflection proxy instances.
 */
@DefaultQualifier(NonNull.class)
public final class ReflectionProxyFactory {
  private final ReflectionRemapper reflectionRemapper;
  private final ClassLoader classLoader;

  private ReflectionProxyFactory(final ReflectionRemapper reflectionRemapper, final ClassLoader classLoader) {
    this.reflectionRemapper = reflectionRemapper;
    this.classLoader = classLoader;
  }

  /**
   * Create a new instance of the given "reflection proxy interface".
   *
   * @param proxyInterface reflection proxy interface class
   * @param <I>            interface type
   * @return reflection proxy instance
   * @throws IllegalArgumentException when the specified proxy interface is malformed or invalid for the current environment
   * @see Proxies
   */
  @SuppressWarnings("unchecked")
  public <I> I reflectionProxy(final Class<I> proxyInterface) {
    return (I) Proxy.newProxyInstance(
      this.classLoader,
      new Class<?>[]{proxyInterface},
      new ReflectionProxyInvocationHandler<>(
        proxyInterface,
        Util.findProxiedClass(proxyInterface, this.reflectionRemapper::remapClassName),
        this.reflectionRemapper
      )
    );
  }

  /**
   * Create a new {@link ReflectionProxyFactory} using the specified
   * {@link ReflectionRemapper} for remapping, and the specified {@link ClassLoader}
   * to load reflection proxy implementation classes.
   *
   * @param reflectionRemapper reflection remapper
   * @param classLoader        classloader
   * @return new {@link ReflectionProxyFactory}
   */
  public static ReflectionProxyFactory create(
    final ReflectionRemapper reflectionRemapper,
    final ClassLoader classLoader
  ) {
    return new ReflectionProxyFactory(reflectionRemapper, classLoader);
  }
}
