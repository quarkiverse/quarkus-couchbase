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
package com.couchbase.quarkus.extension.it;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.quarkus.extension.runtime.ClusterEnvironmentCustomizer;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Singleton
public class PublishOnSchedulerCustomizer implements ClusterEnvironmentCustomizer {

    static final String THREAD_PREFIX = "couchbase-it-publish";

    // An instance field, so the scheduler and its threads start at runtime and not during a native build.
    private final Scheduler scheduler = Schedulers.newBoundedElastic(4, 1000, THREAD_PREFIX);

    @Override
    public void customize(ClusterEnvironment.Builder builder) {
        builder.publishOnScheduler(() -> scheduler);
    }

    @PreDestroy
    void dispose() {
        scheduler.dispose();
    }
}
