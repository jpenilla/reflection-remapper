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
package xyz.jpenilla.reflectionremapper.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Used to specify an alternate type to use instead of the
 * annotated element's type.
 *
 * <p>This is mostly useful when a method
 * has a parameter whose type is not visible during compilation,
 * as it allows referencing a reflection proxy, or fully qualified
 * class name to use instead of {@link Object}, which must be used
 * at compile time for these types.</p>
 */
@DefaultQualifier(NonNull.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Type {
  /**
   * A reflection proxy interface proxying the target class. A value of {@code Object.class}
   * indicates it has not been changed from the default, and that {@link #className()}
   * should be used to lookup the class instead.
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
