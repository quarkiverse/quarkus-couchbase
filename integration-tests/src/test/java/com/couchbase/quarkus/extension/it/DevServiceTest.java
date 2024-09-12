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
package com.couchbase.quarkus.extension.it;

import static com.couchbase.client.java.kv.InsertOptions.insertOptions;
import static com.couchbase.client.java.kv.RemoveOptions.removeOptions;
import static com.couchbase.client.java.query.QueryOptions.queryOptions;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.ExistsResult;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.query.*;

import io.quarkus.test.junit.QuarkusTest;
import reactor.core.publisher.Mono;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DevServiceTest {

    private static Cluster cluster;

    private static Bucket bucket;
    private static Scope scope;
    private static Collection collection;

    private static final String BUCKET_NAME = "quarkusBucket";
    private static final String SCOPE_NAME = "quarkusScope";
    private static final String COLLECTION_NAME = "quarkusCollection";

    public DevServiceTest(Cluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Holds sample content for simple assertions.
     */
    private static final JsonObject FOO_CONTENT = JsonObject
            .create()
            .put("foo", "bar");

    private static String keyspace = String.join(".", BUCKET_NAME, SCOPE_NAME, COLLECTION_NAME);

    @BeforeAll
    void createAndGetKeyspace() {
        // Create Bucket, Scope and Collection
        cluster.buckets().createBucket(BucketSettings.create(BUCKET_NAME));
        bucket = cluster.bucket(BUCKET_NAME);
        bucket.waitUntilReady(Duration.ofSeconds(20));
        bucket.collections().createScope(SCOPE_NAME);
        bucket.collections().createCollection(SCOPE_NAME, COLLECTION_NAME);

        scope = bucket.scope(SCOPE_NAME);
        collection = scope.collection(COLLECTION_NAME);
    }

    @Test
    void insertAndGet() {
        String id = UUID.randomUUID().toString();
        MutationResult insertResult = collection.insert(id, "Hello, World");

        assertTrue(insertResult.cas() != 0);
        assertTrue(insertResult.mutationToken().isPresent());

        GetResult getResult = collection.get(id);
        assertEquals("Hello, World", getResult.contentAs(String.class));
        assertEquals("\"Hello, World\"", new String(getResult.contentAsBytes(), UTF_8));
        assertTrue(getResult.cas() != 0);
        assertFalse(getResult.expiryTime().isPresent());
    }

    @Test
    void exists() {
        String id = UUID.randomUUID().toString();

        assertFalse(collection.exists(id).exists());

        MutationResult insertResult = collection.insert(id, "Hello, World");
        assertTrue(insertResult.cas() != 0);

        ExistsResult existsResult = collection.exists(id);

        assertEquals(insertResult.cas(), existsResult.cas());
        assertTrue(existsResult.exists());
        assertFalse(collection.exists("some_id").exists());
    }

    @Test
    void remove() {
        String id = UUID.randomUUID().toString();

        JsonObject expected = JsonObject.create().put("foo", true);
        MutationResult insert = collection.insert(
                id,
                expected,
                insertOptions().expiry(Duration.ofSeconds(2)));
        assertTrue(insert.cas() != 0);

        assertThrows(
                CasMismatchException.class,
                () -> collection.remove(id, removeOptions().cas(insert.cas() + 100)));

        MutationResult result = collection.remove(id);
        assertTrue(result.cas() != insert.cas());

        assertThrows(DocumentNotFoundException.class, () -> collection.remove(id));
    }

    @Test
    void simpleBlockingSelect() {
        QueryResult result = cluster.query("select 'hello world' as Greeting", queryOptions().metrics(true));

        assertNotNull(result.metaData().requestId());
        assertFalse(result.metaData().clientContextId().isEmpty());
        assertEquals(QueryStatus.SUCCESS, result.metaData().status());
        assertTrue(result.metaData().warnings().isEmpty());
        assertEquals(1, result.rowsAs(JsonObject.class).size());
        assertTrue(result.metaData().signature().isPresent());

        QueryMetrics metrics = result.metaData().metrics().get();
        assertEquals(0, metrics.errorCount());
        assertEquals(0, metrics.warningCount());
        assertEquals(1, metrics.resultCount());
    }

    @Test
    void asyncSelect() throws Exception {
        String id = insertDoc();

        QueryOptions options = queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS);
        CompletableFuture<QueryResult> result = cluster.async().query(
                "select * from " + keyspace + " where meta().id=\"" + id + "\"",
                options);
        List<JsonObject> rows = result.get().rowsAs(JsonObject.class);
        assertEquals(1, rows.size());
    }

    @Test
    void reactiveSelect() {
        String id = insertDoc();

        QueryOptions options = queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS);
        Mono<ReactiveQueryResult> result = cluster.reactive().query(
                "select * from " + keyspace + " where meta().id=\"" + id + "\"",
                options);
        List<JsonObject> rows = result
                .flux()
                .flatMap(ReactiveQueryResult::rowsAsObject)
                .collectList()
                .block();
        assertNotNull(rows);
        assertEquals(1, rows.size());
    }

    /**
     * Inserts a document into the collection and returns the ID of it.
     *
     * It inserts {@link #FOO_CONTENT}.
     */
    private String insertDoc() {
        String id = UUID.randomUUID().toString();
        collection.insert(id, FOO_CONTENT);
        return id;
    }
}
