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
package xyz.jpenilla.reflectionremapper;

import com.volkhart.memory.MemoryMeasurer;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.reflectionremapper.internal.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryMeasureTest {
  @Test
  void testMappingsLoadingAndSize() {
    final long start = System.nanoTime();

    final ReflectionRemapper remapper;
    try (final InputStream mappings = this.getClass().getClassLoader().getResourceAsStream("mappings.tiny")) {
      if (mappings == null) {
        throw new IllegalStateException("mappings.tiny not found");
      }
      remapper = ReflectionRemapper.forPaperReobfMappings(mappings);
    } catch (final IOException ex) {
      Util.sneakyThrow(ex);
      return;
    }

    final long diff = System.nanoTime() - start;
    System.out.println(diff / 1000000000.00D + "s");

    try {
      System.out.println(MemoryMeasurer.measureBytes(remapper) / 1000.0D / 1000.0D + "MB");
    } catch (final UnsupportedClassVersionError ex) {
      new RuntimeException("MemoryMeasurer does not work on this JDK", ex).printStackTrace();
    }

    final String serverPlayer = remapper.remapClassName("net.minecraft.server.level.ServerPlayer");
    assertEquals("net.minecraft.server.level.EntityPlayer", serverPlayer);
  }
}
