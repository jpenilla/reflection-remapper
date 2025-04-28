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

final class MappingNamespace {
  @Deprecated
  public static final String DEOBF = "mojang+yarn";
  @Deprecated
  public static final String OBF = "spigot";
  public static final String MOJANG_PLUS_YARN = "mojang+yarn";
  public static final String MOJANG = "mojang";
  public static final String SPIGOT = "spigot";

  private MappingNamespace() {
  }
}
