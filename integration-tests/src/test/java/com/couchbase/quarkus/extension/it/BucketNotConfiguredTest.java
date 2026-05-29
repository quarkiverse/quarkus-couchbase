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
package com.couchbase.quarkus.extension.it;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.couchbase.client.java.Bucket;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(BucketNotConfiguredTest.NoBucketName.class)
public class BucketNotConfiguredTest {

    @Inject
    Instance<Bucket> bucket;

    @Test
    public void injectingBucketWithoutNameFailsWithClearMessage() {
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

    public static class NoBucketName implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // Unset the bucket name so injecting a Bucket must fail
            return Map.of("quarkus.couchbase.bucket-name", "");
        }
    }
}
