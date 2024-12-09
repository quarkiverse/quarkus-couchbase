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
