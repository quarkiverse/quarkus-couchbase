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
package com.couchbase.quarkus.extension.runtime;

import com.couchbase.client.java.env.ClusterEnvironment;

/**
 * Changes the {@link ClusterEnvironment} of the injected {@code Cluster} before it connects.
 * <p>
 * Declare one or more CDI beans that implement this interface. The extension applies them after the
 * {@code quarkus.couchbase.*} properties, so a customizer can override those values.
 * Customizers run in ascending {@code jakarta.annotation.Priority} order. A customizer without
 * {@code @Priority} has priority 0.
 *
 * <pre>
 * &#64;Singleton
 * public class PublishOnCustomizer implements ClusterEnvironmentCustomizer {
 *     private static final Scheduler SCHEDULER = Schedulers.newBoundedElastic(4, 1000, "my-app");
 *
 *     &#64;Override
 *     public void customize(ClusterEnvironment.Builder builder) {
 *         builder.publishOnScheduler(() -&gt; SCHEDULER);
 *     }
 * }
 * </pre>
 */
public interface ClusterEnvironmentCustomizer {

    void customize(ClusterEnvironment.Builder builder);
}
