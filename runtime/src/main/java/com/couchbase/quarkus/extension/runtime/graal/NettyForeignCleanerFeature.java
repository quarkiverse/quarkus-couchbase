/*
 * Copyright (c) 2026 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.quarkus.extension.runtime.graal;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

/**
 * Registers the FFM downcall descriptors for C {@code malloc} and {@code free} that Netty's
 * {@code CleanerJava24Linker} uses. Netty selects this buffer cleaner in a native image on JDK 25 and
 * later. GraalVM can only create the downcall handles at runtime if the descriptors are registered at
 * build time, and Netty's metadata does not register them.
 *
 * <p>
 * This module compiles with {@code --release 17}, where {@code java.lang.foreign} is not visible, so
 * the descriptors are built by reflection. Without the FFM API this feature does nothing.
 */
public final class NettyForeignCleanerFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        try {
            Class<?> memoryLayout = Class.forName("java.lang.foreign.MemoryLayout");
            Class<?> valueLayout = Class.forName("java.lang.foreign.ValueLayout");
            Class<?> functionDescriptor = Class.forName("java.lang.foreign.FunctionDescriptor");

            // JAVA_LONG models both the size_t argument and the returned pointer.
            Object javaLong = valueLayout.getField("JAVA_LONG").get(null);

            Object argLayouts = Array.newInstance(memoryLayout, 1);
            Array.set(argLayouts, 0, javaLong);
            Class<?> argLayoutsType = argLayouts.getClass();

            // malloc: (long) -> long
            Method of = functionDescriptor.getMethod("of", memoryLayout, argLayoutsType);
            Object mallocDesc = of.invoke(null, new Object[] { javaLong, argLayouts });

            // free: (long) -> void
            Method ofVoid = functionDescriptor.getMethod("ofVoid", argLayoutsType);
            Object freeDesc = ofVoid.invoke(null, new Object[] { argLayouts });

            RuntimeForeignAccess.registerForDowncall(mallocDesc);
            RuntimeForeignAccess.registerForDowncall(freeDesc);
        } catch (ClassNotFoundException e) {
            // No FFM API, so Netty does not use CleanerJava24Linker.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register Netty FFM cleaner downcalls for native image", e);
        }
    }

    @Override
    public String getDescription() {
        return "Registers Netty 4.2 CleanerJava24Linker malloc/free FFM downcalls";
    }
}
