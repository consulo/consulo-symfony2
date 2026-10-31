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

import consulo.application.Application;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The read access of a test body, like the event dispatch thread gives it to the IntelliJ tests the plugin's tests
 * are written for: the body holds the read lock, and lets it go while the project is changed and settles.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
final class TestReadLock {
    // the read lock of the application is internal - the tests run on the class path, where reflection reaches it
    private static Object ourToken;

    private TestReadLock() {
    }

    static void acquire() {
        if (ourToken == null) {
            ourToken = invoke("startRead");
        }
    }

    static void release() {
        Object token = ourToken;
        if (token != null) {
            ourToken = null;
            invoke("endRead", token);
        }
    }

    /**
     * Runs {@code action} - a change of the project and the wait for it to settle - without the read lock of the test
     */
    static <T> T withoutRead(ThrowableSupplier<T> action) throws Exception {
        boolean held = ourToken != null;
        release();
        try {
            return action.get();
        }
        finally {
            if (held) {
                acquire();
            }
        }
    }

    interface ThrowableSupplier<T> {
        T get() throws Exception;
    }

    private static Object invoke(String name, Object... arguments) {
        try {
            Object lock = lock();
            for (Method method : lock.getClass().getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
                    method.setAccessible(true);
                    return method.invoke(lock, arguments);
                }
            }
            throw new AssertionError("no " + name + " in " + lock.getClass());
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object lock() throws ReflectiveOperationException {
        for (Class<?> type = Application.get().getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("myLock");
                field.setAccessible(true);
                return field.get(Application.get());
            }
            catch (NoSuchFieldException ignored) {
            }
        }
        throw new AssertionError("no read lock in " + Application.get().getClass());
    }
}
