/*
 * Copyright (c) 2025 Couchbase, Inc.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.couchbase.client.java.Bucket;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that injecting the {@code Bucket} bean without configuring a bucket name fails with a
 * clear message. The name is validated before any cluster connection, so no Couchbase server is
 * needed: credentials are set only to make the config valid, and DevServices stays off.
 */
public class BucketBeanConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.couchbase.username", "test")
            .overrideConfigKey("quarkus.couchbase.password", "test");

    @Inject
    Instance<Bucket> bucket;

    @Test
    void injectingBucketWithoutNameFailsWithClearMessage() {
        // The Bucket bean is a lazy ApplicationScoped proxy; invoking a method triggers creation.
        var exception = assertThrows(RuntimeException.class, () -> bucket.get().name());

        boolean hasClearMessage = false;
        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("quarkus.couchbase.bucket-name is required")) {
                hasClearMessage = true;
                break;
            }
        }
        assertTrue(hasClearMessage, "Expected a clear bucket-name error, but got: " + exception);
    }
}
