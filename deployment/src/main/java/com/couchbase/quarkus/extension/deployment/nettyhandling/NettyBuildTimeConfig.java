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
package com.couchbase.quarkus.extension.deployment.nettyhandling;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "quarkus-embev", phase = ConfigPhase.BUILD_TIME)
public class NettyBuildTimeConfig {

    /**
     * The value configuring the {@code io.netty.allocator.maxOrder} system property of Netty.
     * The default value is {@code 3}.
     *
     * Configuring this property overrides the minimum {@code maxOrder} requested by the extensions.
     *
     * This property affects the memory consumption of the application.
     * It must be used carefully.
     * More details on https://programmer.group/pool-area-of-netty-memory-pool.html.
     */
    @ConfigItem
    public OptionalInt allocatorMaxOrder;
}
