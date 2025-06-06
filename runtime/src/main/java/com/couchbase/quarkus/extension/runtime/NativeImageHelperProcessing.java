/*
 * Copyright (c) 2024 Couchbase, Inc.
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
package com.couchbase.quarkus.extension.runtime;

import java.util.Queue;

import com.couchbase.client.core.deps.org.jctools.queues.atomic.unpadded.MpscAtomicUnpaddedArrayQueue;
import com.couchbase.client.core.util.NativeImageHelper;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class NativeImageHelperProcessing {
}

@TargetClass(value = NativeImageHelper.class)
final class Target_NativeImageHelper {
    @Substitute
    public static <T> Queue<T> createMpscArrayQueue(int capacity) {
        return new MpscAtomicUnpaddedArrayQueue<>(capacity);
    }
}
