/*
 * reflection-remapper
 *
 * Copyright (c) 2021-2024 Jason Penilla
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
package xyz.jpenilla.reflectionremapper.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Used to specify that an interface is a "reflection proxy interface"
 * for the specified class.
 *
 * <p>The target class can be specified using it's {@link Class} object, or for private/inaccessible
 * classes, by the fully qualified class name.</p>
 */
@DefaultQualifier(NonNull.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxies {
  /**
   * The target class. A value of {@code Object.class} indicates it has
   * not been changed from the default, and that {@link #className()} should be
   * used to lookup the class instead.
   *
   * @return target class
   */
  Class<?> value() default Object.class;

  /**
   * The fully qualified name of the target class. A value of {@code ""} (empty string) indicates
   * it has not been changed from the default, and that {@link #value()} should be
   * used instead.
   *
   * @return target class name
   */
  String className() default "";
}
