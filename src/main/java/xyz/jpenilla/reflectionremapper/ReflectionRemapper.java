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
package xyz.jpenilla.reflectionremapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static xyz.jpenilla.reflectionremapper.internal.util.Util.mojangMapped;

/**
 * Interface providing methods for remapping class, method, and field names from one
 * mappings namespace to another.
 *
 * <p>Particularly useful for reflection, as it's not remapped by bytecode remapping.</p>
 *
 * <p>The various standard {@link ReflectionRemapper ReflectionRemappers} can be created
 * using the provided static factory methods.</p>
 */
@DefaultQualifier(NonNull.class)
public interface ReflectionRemapper {
  /**
   * Remaps a Mojang-mapped class name to its current runtime name.
   *
   * @param className fully qualified class name
   * @return fully qualified runtime class name
   */
  String remapClassName(String className);

  /**
   * Remaps a Mojang-mapped field name to its current runtime name.
   *
   * @param holdingClass class declaring the field
   * @param fieldName    field name
   * @return runtime field name
   */
  String remapFieldName(Class<?> holdingClass, String fieldName);

  /**
   * Remaps a Mojang-mapped method name to its current runtime name.
   *
   * @param holdingClass class declaring the method
   * @param methodName   method name
   * @param paramTypes   parameter types
   * @return runtime method name
   */
  String remapMethodName(Class<?> holdingClass, String methodName, Class<?>... paramTypes);

  /**
   * Remaps a Mojang-mapped class or array name (as given to Class.forName(String)) to
   * its current runtime name using {@link #remapClassName(String)}.
   *
   * @param name class or array name
   * @return remapped name
   */
  default String remapClassOrArrayName(final String name) {
    Objects.requireNonNull(name, "name");
    if (name.isEmpty()) {
      return name;
    }

    // Array type
    if (name.charAt(0) == '[') {
      final int last = name.lastIndexOf('[');

      try {
        // Object array type
        if (name.charAt(last + 1) == 'L') {
          final String cls = name.substring(last + 2, name.length() - 1);
          return name.substring(0, last + 2) + this.remapClassName(cls) + ';';
        }
      } catch (final IndexOutOfBoundsException ex) {
        // Pass through on invalid names
        return name;
      }

      // Primitive array
      return name;
    }

    return this.remapClassName(name);
  }

  /**
   * Returns a noop {@link ReflectionRemapper} instance which simply passes through the given
   * names without remapping.
   *
   * @return noop reflection remapper
   */
  static ReflectionRemapper noop() {
    return NoopReflectionRemapper.INSTANCE;
  }

  /**
   * Creates a new {@link ReflectionRemapper} from the given mappings.
   *
   * <p>Note that this does not close the provided {@link InputStream}.</p>
   *
   * @param mappings      mappings
   * @param fromNamespace from namespace
   * @param toNamespace   to namespace
   * @return reflection remapper
   */
  static ReflectionRemapper forMappings(
    final InputStream mappings,
    final String fromNamespace,
    final String toNamespace
  ) {
    try {
      final MemoryMappingTree tree = new MemoryMappingTree(true);
      tree.setSrcNamespace(fromNamespace);
      tree.setDstNamespaces(new ArrayList<>(Collections.singletonList(toNamespace)));

      MappingReader.read(new InputStreamReader(mappings, StandardCharsets.UTF_8), tree);

      return ReflectionRemapperImpl.fromMappingTree(tree, fromNamespace, toNamespace);
    } catch (final IOException ex) {
      throw new RuntimeException("Failed to read mappings.", ex);
    }
  }

  /**
   * Creates a new {@link ReflectionRemapper} from the given mappings.
   *
   * @param mappings      mappings
   * @param fromNamespace from namespace
   * @param toNamespace   to namespace
   * @return reflection remapper
   */
  static ReflectionRemapper forMappings(
    final Path mappings,
    final String fromNamespace,
    final String toNamespace
  ) {
    try (final InputStream stream = Files.newInputStream(mappings)) {
      return forMappings(stream, fromNamespace, toNamespace);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new {@link ReflectionRemapper} using the provided Paper reobfuscation mappings.
   *
   * <p>If the current environment is detected to be Mojang-mapped, {@link #noop()} will be
   * returned rather than reading the mappings and creating an actual remapping {@link ReflectionRemapper}.</p>
   *
   * @param mappings reobf mappings
   * @return reflection remapper
   */
  static ReflectionRemapper forPaperReobfMappings(final Path mappings) {
    if (mojangMapped()) {
      return noop();
    }

    return forMappings(mappings, MappingNamespace.DEOBF, MappingNamespace.OBF);
  }

  /**
   * Creates a new {@link ReflectionRemapper} using the provided Paper reobfuscation mappings.
   *
   * <p>If the current environment is detected to be Mojang-mapped, {@link #noop()} will be
   * returned rather than reading the mappings and creating an actual remapping {@link ReflectionRemapper}.</p>
   *
   * <p>Note that this does not close the provided InputStream.</p>
   *
   * @param mappings reobf mappings
   * @return reflection remapper
   */
  static ReflectionRemapper forPaperReobfMappings(final InputStream mappings) {
    if (mojangMapped()) {
      return noop();
    }

    return forMappings(mappings, MappingNamespace.DEOBF, MappingNamespace.OBF);
  }

  /**
   * Creates a new {@link ReflectionRemapper} using the reobf mappings file inside reobfuscated
   * Paper jars.
   *
   * <p>If the current environment is detected to be Mojang-mapped, {@link #noop()} will be
   * returned rather than reading the mappings and creating an actual remapping {@link ReflectionRemapper}.</p>
   *
   * @return reflection remapper
   */
  static ReflectionRemapper forReobfMappingsInPaperJar() {
    if (mojangMapped()) {
      return noop();
    }

    final Class<?> bukkitClass;
    final Method getServerMethod;
    final Class<?> craftServerClass;
    try {
      bukkitClass = Class.forName("org.bukkit.Bukkit");
      getServerMethod = bukkitClass.getDeclaredMethod("getServer");
      craftServerClass = getServerMethod.invoke(null).getClass();
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    try (final @Nullable InputStream reobfIn = craftServerClass.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny")) {
      if (reobfIn == null) {
        throw new IllegalStateException("Could not find mappings in expected location.");
      }
      return forPaperReobfMappings(reobfIn);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
