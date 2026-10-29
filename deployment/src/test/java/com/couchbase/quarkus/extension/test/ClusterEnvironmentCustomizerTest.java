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
package com.couchbase.quarkus.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.quarkus.extension.runtime.ClusterEnvironmentCustomizer;

import io.quarkus.test.QuarkusUnitTest;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Verifies that {@link ClusterEnvironmentCustomizer} beans change the environment of the injected
 * {@code Cluster}. The SDK connects in the background, so no Couchbase server is needed and
 * DevServices stays off.
 */
public class ClusterEnvironmentCustomizerTest {

    static final Supplier<Scheduler> SCHEDULER = Schedulers::boundedElastic;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PublishOnCustomizer.class, LowPriorityTimeoutCustomizer.class,
                            HighPriorityTimeoutCustomizer.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.couchbase.connection-string", "localhost")
            .overrideConfigKey("quarkus.couchbase.username", "test")
            .overrideConfigKey("quarkus.couchbase.password", "test");

    @Inject
    Cluster cluster;

    @Test
    void customizerSetsPublishOnScheduler() {
        assertSame(SCHEDULER, cluster.environment().userScheduler());
    }

    // The names sort against the priorities, so this fails if Arc order or descending order wins.
    @Test
    void customizersRunInPriorityOrder() {
        assertEquals(Duration.ofSeconds(7), cluster.environment().timeoutConfig().kvTimeout());
    }

    @Singleton
    public static class PublishOnCustomizer implements ClusterEnvironmentCustomizer {
        @Override
        public void customize(ClusterEnvironment.Builder builder) {
            builder.publishOnScheduler(SCHEDULER);
        }
    }

    @Singleton
    @Priority(20)
    public static class HighPriorityTimeoutCustomizer implements ClusterEnvironmentCustomizer {
        @Override
        public void customize(ClusterEnvironment.Builder builder) {
            builder.timeoutConfig(timeouts -> timeouts.kvTimeout(Duration.ofSeconds(7)));
        }
    }

    @Singleton
    @Priority(10)
    public static class LowPriorityTimeoutCustomizer implements ClusterEnvironmentCustomizer {
        @Override
        public void customize(ClusterEnvironment.Builder builder) {
            builder.timeoutConfig(timeouts -> timeouts.kvTimeout(Duration.ofSeconds(5)));
        }
    }
}
