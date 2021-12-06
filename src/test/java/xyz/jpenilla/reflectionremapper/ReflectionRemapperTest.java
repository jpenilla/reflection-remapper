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

import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflectionRemapperTest {
  private ReflectionProxyFactory factory() {
    return ReflectionProxyFactory.create(
      ReflectionRemapper.noop(),
      this.getClass().getClassLoader()
    );
  }

  @Proxies(String.class)
  interface StringProxy {
    char[] toCharArray(String instance);
  }

  @Test
  void testStringProxy() {
    final StringProxy stringProxy = this.factory().reflectionProxy(StringProxy.class);
    final String string = "SomeString";
    assertArrayEquals(string.toCharArray(), stringProxy.toCharArray(string));
  }

  @Test
  void tests() {
    final String secretString = "Secret string 123";
    final PrivateClass privateClass = new PrivateClass(secretString);
    final PrivateClassProxy privateClassProxy = this.factory().reflectionProxy(PrivateClassProxy.class);
    assertEquals(secretString, privateClassProxy.secret(privateClass));

    final AnotherPrivateClass anotherPrivateClass = new AnotherPrivateClass();
    assertEquals(privateClass.useSecretClass(anotherPrivateClass), privateClassProxy.useSecretClass(privateClass, anotherPrivateClass));
    assertEquals(privateClass.useSecretClass(anotherPrivateClass), privateClassProxy.useSecretClass0(privateClass, anotherPrivateClass));

    final String newSecret = "New secret string!";
    privateClassProxy.setSecret(privateClass, newSecret);
    assertEquals(newSecret, privateClass.secret());
    assertEquals(privateClass.secret(), privateClassProxy.getSecret(privateClass));

    assertEquals(PrivateClass.staticMethod(), privateClassProxy.staticMethod());

    final String staticFieldNewValue = "value in static field";
    privateClassProxy.setStaticField(staticFieldNewValue);
    assertEquals(staticFieldNewValue, PrivateClass.static_field);
    assertEquals(PrivateClass.static_field, privateClassProxy.getStaticField());
  }

  @Test
  void testConstructor() {
    final PrivateClassProxy proxy = this.factory().reflectionProxy(PrivateClassProxy.class);
    final String expected = "abc123xyz";
    final PrivateClass instance = (PrivateClass) proxy.construct(expected);
    assertEquals(expected, instance.secret());
  }

  @Test
  void testSynthetics() {
    final PrivateClassProxy proxy = this.factory().reflectionProxy(PrivateClassProxy.class);
    assertEquals("nothing5", proxy.get(() -> "nothing", 5).get());
  }

  @Proxies(className = "xyz.jpenilla.reflectionremapper.ReflectionRemapperTest$PrivateClass")
  interface PrivateClassProxy {
    String secret(Object instance);

    String useSecretClass(
      Object instance,
      @Type(className = "xyz.jpenilla.reflectionremapper.ReflectionRemapperTest$AnotherPrivateClass") Object anotherPrivateClass
    );

    @MethodName("useSecretClass")
    String useSecretClass0(
      Object instance,
      @Type(AnotherPrivateClassProxy.class) Object anotherPrivateClass
    );

    @FieldGetter("secret")
    String getSecret(Object instance);

    @FieldSetter("secret")
    void setSecret(
      Object instance,
      String value
    );

    @Static
    @FieldSetter("static_field")
    void setStaticField(String value);

    @Static
    @FieldGetter("static_field")
    String getStaticField();

    @Static
    int staticMethod();

    @ConstructorInvoker
    Object construct(String secret);

    default Supplier<String> get(final Supplier<String> s, final int number) {
      return () -> s.get() + number; // will create a synthetic method
    }
  }

  private static final class PrivateClass {
    private static String static_field;
    private final String secret;

    private PrivateClass(final String secret) {
      this.secret = secret;
    }

    private String secret() {
      return this.secret;
    }

    private String useSecretClass(final AnotherPrivateClass secret) {
      return secret + " is the toString() of secret!";
    }

    private static int staticMethod() {
      return 100;
    }
  }

  @Proxies(className = "xyz.jpenilla.reflectionremapper.ReflectionRemapperTest$AnotherPrivateClass")
  interface AnotherPrivateClassProxy {
  }

  private static final class AnotherPrivateClass {
    private AnotherPrivateClass() {
    }
  }
}
