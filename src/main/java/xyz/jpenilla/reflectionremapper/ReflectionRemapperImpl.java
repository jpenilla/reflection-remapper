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
package xyz.jpenilla.reflectionremapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.mappingio.tree.MappingTree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.internal.util.StringPool;

@DefaultQualifier(NonNull.class)
final class ReflectionRemapperImpl implements ReflectionRemapper {
  private final Map<String, ClassMapping> mappingsByObf;
  private final Map<String, ClassMapping> mappingsByDeobf;

  private ReflectionRemapperImpl(final Set<ClassMapping> mappings) {
    this.mappingsByObf = mappings.stream().collect(Collectors.toUnmodifiableMap(ClassMapping::obfName, map -> map));
    this.mappingsByDeobf = mappings.stream().collect(Collectors.toUnmodifiableMap(ClassMapping::deobfName, map -> map));
  }

  @Override
  public String remapClassName(final String className) {
    final @Nullable ClassMapping map = this.mappingsByDeobf.get(className);
    if (map == null) {
      return className;
    }
    return map.obfName();
  }

  @Override
  public String remapFieldName(final Class<?> holdingClass, final String fieldName) {
    final @Nullable ClassMapping clsMap = this.mappingsByObf.get(holdingClass.getName());
    if (clsMap == null) {
      return fieldName;
    }
    return clsMap.fieldsDeobfToObf().get(fieldName);
  }

  @Override
  public String remapMethodName(final Class<?> holdingClass, final String methodName, final Class<?>... paramTypes) {
    final @Nullable ClassMapping clsMap = this.mappingsByObf.get(holdingClass.getName());
    if (clsMap == null) {
      return methodName;
    }
    return clsMap.methods().get(methodKey(methodName, paramTypes));
  }

  private static String methodKey(final String deobfName, final Class<?>... paramTypes) {
    return deobfName + paramsDescriptor(paramTypes);
  }

  private static String methodKey(final String deobfName, final String obfMethodDesc) {
    return deobfName + paramsDescFromMethodDesc(obfMethodDesc);
  }

  private static String paramsDescriptor(final Class<?>... params) {
    final StringBuilder builder = new StringBuilder();
    for (final Class<?> param : params) {
      builder.append(param.descriptorString());
    }
    return builder.toString();
  }

  private static String paramsDescFromMethodDesc(final String methodDescriptor) {
    String ret = methodDescriptor.substring(1);
    ret = ret.substring(0, ret.indexOf(")"));
    return ret;
  }

  private record ClassMapping(
    String obfName,
    String deobfName,
    Map<String, String> fieldsDeobfToObf,
    Map<String, String> methods // deobfMethodName + obfParamsDescriptor -> obfMethodName
  ) {}

  static ReflectionRemapperImpl fromMappingTree(
    final MappingTree tree,
    final String fromNamespace,
    final String toNamespace
  ) {
    final StringPool pool = new StringPool();

    final Set<ClassMapping> mappings = new HashSet<>();

    for (final MappingTree.ClassMapping cls : tree.getClasses()) {
      final Map<String, String> fields = new HashMap<>();
      for (final MappingTree.FieldMapping field : cls.getFields()) {
        fields.put(
          pool.string(field.getName(fromNamespace)),
          pool.string(field.getName(toNamespace))
        );
      }

      final Map<String, String> methods = new HashMap<>();
      for (final MappingTree.MethodMapping method : cls.getMethods()) {
        methods.put(
          pool.string(methodKey(method.getName(fromNamespace), method.getDesc(toNamespace))),
          pool.string(method.getName(toNamespace))
        );
      }

      final ClassMapping map = new ClassMapping(
        cls.getName(toNamespace).replace('/', '.'),
        cls.getName(fromNamespace).replace('/', '.'),
        Map.copyOf(fields),
        Map.copyOf(methods)
      );

      mappings.add(map);
    }

    return new ReflectionRemapperImpl(mappings);
  }
}
