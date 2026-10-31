/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.adrienbrault.idea.symfony2plugin.tests;

import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * The assertions of JUnit 3 and IntelliJ's {@code UsefulTestCase} the plugin's tests are written with, the message first.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
public abstract class JUnit3Assert {
    public static void fail(String message) {
        Assertions.fail(message);
    }

    public static void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }

    public static void assertTrue(String message, boolean condition) {
        Assertions.assertTrue(condition, message);
    }

    public static void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }

    public static void assertFalse(String message, boolean condition) {
        Assertions.assertFalse(condition, message);
    }

    public static void assertNull(Object object) {
        Assertions.assertNull(object);
    }

    public static void assertNull(String message, Object object) {
        Assertions.assertNull(object, message);
    }

    public static void assertNotNull(Object object) {
        Assertions.assertNotNull(object);
    }

    public static void assertNotNull(String message, Object object) {
        Assertions.assertNotNull(object, message);
    }

    public static void assertEquals(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, message);
    }

    public static void assertEquals(long expected, long actual) {
        Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(String message, long expected, long actual) {
        Assertions.assertEquals(expected, actual, message);
    }

    public static void assertNotSame(Object unexpected, Object actual) {
        Assertions.assertNotSame(unexpected, actual);
    }

    public static void assertSame(Object expected, Object actual) {
        Assertions.assertSame(expected, actual);
    }

    public static <T> void assertInstanceOf(Object object, Class<T> aClass) {
        Assertions.assertInstanceOf(aClass, object);
    }

    @SafeVarargs
    public static <T> void assertContainsElements(Collection<? extends T> collection, T... expected) {
        assertContainsElements(collection, Arrays.asList(expected));
    }

    public static <T> void assertContainsElements(Collection<? extends T> collection, Collection<T> expected) {
        for (T element : expected) {
            if (!collection.contains(element)) {
                fail("expected " + element + " in " + collection);
            }
        }
    }

    @SafeVarargs
    public static <T> void assertDoesntContain(Collection<? extends T> collection, T... notExpected) {
        for (T element : notExpected) {
            if (collection.contains(element)) {
                fail("unexpected " + element + " in " + collection);
            }
        }
    }

    @SafeVarargs
    public static <T> void assertSameElements(Collection<? extends T> collection, T... expected) {
        Assertions.assertEquals(new HashSet<>(Arrays.asList(expected)), new HashSet<>(collection));
    }

    public static <T> void assertSameElements(Collection<? extends T> collection, Collection<T> expected) {
        Assertions.assertEquals(new HashSet<>(expected), new HashSet<>(collection));
    }

    @SafeVarargs
    public static <T> void assertOrderedEquals(Collection<? extends T> collection, T... expected) {
        Assertions.assertEquals(Arrays.asList(expected), List.copyOf(collection));
    }

    public static void assertSize(int expectedSize, Object[] array) {
        Assertions.assertEquals(expectedSize, array.length, () -> Arrays.toString(array));
    }

    public static void assertSize(int expectedSize, Collection<?> collection) {
        Assertions.assertEquals(expectedSize, collection.size(), () -> String.valueOf(collection));
    }

    public static void assertEmpty(Object[] array) {
        assertSize(0, array);
    }

    public static void assertEmpty(Collection<?> collection) {
        assertSize(0, collection);
    }

    public static void assertNotEmpty(Collection<?> collection) {
        Assertions.assertFalse(collection.isEmpty());
    }

    public static void assertEmpty(String message, Collection<?> collection) {
        Assertions.assertTrue(collection.isEmpty(), message + ": " + collection);
    }

    public static void assertContainsOrdered(Collection<?> collection, Object... expected) {
        int index = 0;
        for (Object element : collection) {
            if (index < expected.length && Objects.equals(element, expected[index])) {
                index++;
            }
        }
        Assertions.assertEquals(expected.length, index, () -> "expected in order " + Arrays.toString(expected) + " in " + collection);
    }

    public static void assertArrayEquals(Object[] expected, Object[] actual) {
        Assertions.assertArrayEquals(expected, actual);
    }

    public static void assertArrayEquals(String message, Object[] expected, Object[] actual) {
        Assertions.assertArrayEquals(expected, actual, message);
    }
}
