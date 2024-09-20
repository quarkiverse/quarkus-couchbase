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

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "couchbase", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CouchbaseConfig {
    /**
     * The connection string, e.g. "couchbase://10.202.32.32" or "localhost".
     */
    @ConfigItem
    public String connectionString;

    /**
     * The username to authenticate with.
     */
    @ConfigItem
    public String username;

    /**
     * The password to authenticate with.
     */
    @ConfigItem
    public String password;
    /**
     * The version of the Couchbase server to connect to during the dev/test phase as dev service.
     * Default is latest, see https://hub.docker.com/_/couchbase for available versions.
     */
    @ConfigItem(defaultValue = "latest")
    public String version;
}
