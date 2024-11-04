package com.couchbase.quarkus.extension.runtime;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jctools.queues.atomic.unpadded.MpscAtomicUnpaddedArrayQueue;

import com.couchbase.client.core.cnc.EventBus;
import com.couchbase.client.core.cnc.OrphanReporter;
import com.couchbase.client.core.env.OrphanReporterConfig;
import com.couchbase.client.core.msg.Request;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class OrphanReporterSubstitutions {
}

@TargetClass(OrphanReporter.class)
final class Target_OrphanReporter {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static String ORPHAN_TREAD_PREFIX = "cb-orphan-";
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static AtomicInteger ORPHAN_REPORTER_ID = new AtomicInteger();
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
    //    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    volatile Thread worker = null; // visible for testing
    @Alias
    private Queue<Request<?>> orphanQueue;
    @Alias
    private Duration emitInterval;
    @Alias
    private int sampleSize;
    @Alias
    private EventBus eventBus;
    @Alias
    private boolean enabled;
    @Alias
    private OrphanReporterConfig config;

    @Substitute
    public Target_OrphanReporter(final EventBus eventBus, final OrphanReporterConfig config) {
        this.eventBus = eventBus;
        this.orphanQueue = new MpscAtomicUnpaddedArrayQueue<>(config.queueLength());
        this.emitInterval = config.emitInterval();
        this.sampleSize = config.sampleSize();
        this.enabled = config.enabled();
        this.config = config;

        // Spawn a thread only if the reporter is enabled.
        if (enabled) {
            worker = new Thread(new Target_OrphanReporter.Target_Worker());
            worker.setDaemon(true);
            worker.setName(ORPHAN_TREAD_PREFIX + ORPHAN_REPORTER_ID.incrementAndGet());
        }
    }

    @Alias
    public OrphanReporterConfig config() {
        return config;
    }

    @TargetClass(value = OrphanReporter.class, innerClass = "Worker")
    private static final class Target_Worker implements Runnable {
        @Alias
        public void run() {
        }
    }
}
