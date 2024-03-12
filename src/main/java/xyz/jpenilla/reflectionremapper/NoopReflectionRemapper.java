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
package xyz.jpenilla.reflectionremapper;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class NoopReflectionRemapper implements ReflectionRemapper {
  static NoopReflectionRemapper INSTANCE = new NoopReflectionRemapper();

  private NoopReflectionRemapper() {
  }

  @Override
  public String remapClassName(final String className) {
    return className;
  }

  @Override
  public String remapFieldName(final Class<?> holdingClass, final String fieldName) {
    return fieldName;
  }

  @Override
  public String remapMethodName(final Class<?> holdingClass, final String methodName, final Class<?>... paramTypes) {
    return methodName;
  }

  @Override
  public String remapClassOrArrayName(final String name) {
    return name;
  }
}
