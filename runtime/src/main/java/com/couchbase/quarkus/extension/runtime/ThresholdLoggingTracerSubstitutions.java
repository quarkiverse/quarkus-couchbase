package com.couchbase.quarkus.extension.runtime;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.couchbase.client.core.cnc.EventBus;
import com.couchbase.client.core.cnc.tracing.ThresholdLoggingTracer;
import com.couchbase.client.core.deps.org.jctools.queues.atomic.unpadded.MpscAtomicUnpaddedArrayQueue;
import com.couchbase.client.core.env.ThresholdLoggingTracerConfig;
import com.couchbase.client.core.msg.Request;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class ThresholdLoggingTracerSubstitutions {
}

@TargetClass(value = ThresholdLoggingTracer.class)
final class Target_ThresholdLoggingTracer {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static AtomicInteger REQUEST_TRACER_ID = new AtomicInteger();
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_TOTAL_MICROS = "total_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_DISPATCH_MICROS = "last_dispatch_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_TOTAL_DISPATCH_MICROS = "total_dispatch_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_ENCODE_MICROS = "encode_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_SERVER_MICROS = "last_server_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_TOTAL_SERVER_MICROS = "total_server_duration_us";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_OPERATION_ID = "operation_id";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_OPERATION_NAME = "operation_name";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_LAST_LOCAL_SOCKET = "last_local_socket";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_LAST_REMOTE_SOCKET = "last_remote_socket";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_LAST_LOCAL_ID = "last_local_id";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static String KEY_TIMEOUT = "timeout_ms";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private AtomicBoolean running = new AtomicBoolean(false);
    @Alias
    private Queue<Request<?>> overThresholdQueue;
    @Alias
    private EventBus eventBus;
    @Alias
    private Thread worker;
    @Alias
    private ThresholdLoggingTracerConfig config;
    @Alias
    private long kvThreshold;
    @Alias
    private long queryThreshold;
    @Alias
    private long viewThreshold;
    @Alias
    private long searchThreshold;
    @Alias
    private long analyticsThreshold;
    @Alias
    private long transactionsThreshold;
    @Alias
    private Duration emitInterval;
    @Alias
    private int sampleSize;

    @Substitute
    private Target_ThresholdLoggingTracer(final EventBus eventBus, ThresholdLoggingTracerConfig config) {
        this.eventBus = eventBus;
        this.overThresholdQueue = new MpscAtomicUnpaddedArrayQueue<>(config.queueLength());
        kvThreshold = config.kvThreshold().toNanos();
        analyticsThreshold = config.analyticsThreshold().toNanos();
        searchThreshold = config.searchThreshold().toNanos();
        viewThreshold = config.viewThreshold().toNanos();
        queryThreshold = config.queryThreshold().toNanos();
        transactionsThreshold = config.transactionsThreshold().toNanos();
        sampleSize = config.sampleSize();
        emitInterval = config.emitInterval();
        this.config = config;

        worker = new Thread(new Target_ThresholdLoggingTracer.Target_Worker());
        worker.setDaemon(true);
    }

    @TargetClass(value = ThresholdLoggingTracer.class, innerClass = "Worker")
    private static final class Target_Worker implements Runnable {
        @Alias
        public void run() {
        }
    }
}
