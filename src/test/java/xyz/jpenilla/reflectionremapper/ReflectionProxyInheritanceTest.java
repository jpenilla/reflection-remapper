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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionProxyInheritanceTest {
  private ReflectionProxyFactory factory() {
    return ReflectionProxyFactory.create(
      ReflectionRemapper.noop(),
      this.getClass().getClassLoader()
    );
  }

  @Proxies(String.class)
  interface StringProxy {}

  @Proxies(Path.class)
  interface InvalidPathProxy extends StringProxy {}

  @Test
  void testInvalidHierarchy() {
    // Path does not extend String
    assertThrows(IllegalArgumentException.class, () -> this.factory().reflectionProxy(InvalidPathProxy.class));
  }

  static class Level {
    final int number = 50;

    String name() {
      return this.level();
    }

    String level() {
      return Level.class.getName();
    }
  }

  static class ServerLevel extends Level {
    final int number1 = 55;

    @Override
    String name() {
      return this.serverLevel();
    }

    String serverLevel() {
      return ServerLevel.class.getName();
    }
  }

  @Proxies(Level.class)
  interface LevelProxy {
    String name(Level instance);

    @FieldGetter("number")
    int number(Level instance);

    default String test0() {
      return "LP 0";
    }

    default String test1() {
      return "LP 1";
    }
  }

  @Proxies(ServerLevel.class)
  interface ServerLevelProxy extends LevelProxy {
    @FieldGetter("number1")
    int number1(ServerLevel level);

    @Override
    default String test1() {
      return "S" + LevelProxy.super.test1();
    }

    default String test2() {
      return "SLP 2";
    }
  }

  @Test
  void testValidHierarchy() {
    final LevelProxy levelProxy = this.factory().reflectionProxy(LevelProxy.class);
    final ServerLevelProxy serverLevelProxy = this.factory().reflectionProxy(ServerLevelProxy.class);

    final ServerLevel sl = new ServerLevel();
    final Level l = new Level();

    assertEquals(levelProxy.name(l), serverLevelProxy.name(l));
    assertEquals(levelProxy.name(sl), serverLevelProxy.name(sl));
    assertEquals(levelProxy.number(sl), 50);
    assertEquals(levelProxy.number(sl), serverLevelProxy.number(sl));
    assertEquals(serverLevelProxy.number1(sl), 55);

    // test default methods on proxy interfaces
    assertEquals(levelProxy.test0(), serverLevelProxy.test0());
    assertEquals("S" + levelProxy.test1(), serverLevelProxy.test1());
    assertEquals("SLP 2", serverLevelProxy.test2());
  }
}
