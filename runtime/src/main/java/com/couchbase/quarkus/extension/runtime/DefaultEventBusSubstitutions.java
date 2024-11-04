package com.couchbase.quarkus.extension.runtime;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.couchbase.client.core.cnc.DefaultEventBus;
import com.couchbase.client.core.cnc.Event;
import com.couchbase.client.core.deps.org.jctools.queues.atomic.unpadded.MpscAtomicUnpaddedArrayQueue;
import com.couchbase.client.core.util.NanoTimestamp;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import reactor.core.scheduler.Scheduler;

public class DefaultEventBusSubstitutions {
}

@TargetClass(DefaultEventBus.class)
final class Target_DefaultEventBus {

    @Alias
    private CopyOnWriteArraySet<Consumer<Event>> subscribers = new CopyOnWriteArraySet<>();
    @Alias
    private Queue<Event> eventQueue;
    @Alias
    private AtomicBoolean running = new AtomicBoolean(false);
    @Alias
    private PrintStream errorLogging;

    @Alias
    private String threadName;

    @Alias
    private Duration idleSleepDuration;

    @Alias
    private Duration overflowLogInterval;

    @Alias
    private Scheduler scheduler;

    @Alias
    private volatile Thread runningThread;

    @Alias
    private volatile NanoTimestamp overflowLogTimestamp = NanoTimestamp.never();
    @Alias
    private Map<Class<? extends Event>, Target_SampleEventAndCount> overflowInfo = new ConcurrentHashMap<>();

    @Alias
    public static Target_Builder builder(final Scheduler scheduler) {
        return new Target_Builder(scheduler);
    }

    @Alias
    public static Target_DefaultEventBus create(final Scheduler scheduler) {
        return builder(scheduler).build();
    }

    @Substitute
    private Target_DefaultEventBus(Target_Builder builder) {
        this.eventQueue = new MpscAtomicUnpaddedArrayQueue<>(builder.queueCapacity);
        this.scheduler = builder.scheduler;
        this.errorLogging = builder.errorLogging.orElse(null);
        this.threadName = builder.threadName;
        this.idleSleepDuration = builder.idleSleepDuration;
        this.overflowLogInterval = builder.overflowLogInterval;
    }

    @TargetClass(value = DefaultEventBus.class, innerClass = "Builder")
    static final class Target_Builder {
        @Alias
        Scheduler scheduler;

        @Alias
        int queueCapacity;

        @Alias
        Optional<PrintStream> errorLogging;

        @Alias
        String threadName;

        @Alias
        Duration idleSleepDuration;

        @Alias
        Duration overflowLogInterval;

        @Alias
        Target_Builder(final Scheduler scheduler) {
        }

        @Alias
        public Target_DefaultEventBus build() {
            return new Target_DefaultEventBus(this);
        }
    }

    @TargetClass(value = DefaultEventBus.class, innerClass = "SampleEventAndCount")
    static final class Target_SampleEventAndCount {

    }
}
