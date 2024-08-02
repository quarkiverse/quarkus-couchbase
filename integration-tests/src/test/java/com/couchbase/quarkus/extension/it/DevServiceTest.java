package com.couchbase.quarkus.extension.it;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionSpec;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DevServiceTest {

    Cluster cluster;

    public DevServiceTest(Cluster cluster) {
        this.cluster = cluster;
    }

    @Test
    void createBucketTest() {
        // tests creating a bucket and waiting for it to be ready
        cluster.buckets().createBucket(BucketSettings.create("testBucket"));
        cluster.bucket("testBucket").waitUntilReady(Duration.ofMinutes(2));
        Assertions.assertNotNull(cluster.buckets().getAllBuckets().get("testBucket"));
        cluster.buckets().dropBucket("testBucket");
    }

    @Test
    void insertDocument() {
        // this is an adapted example from the couchbase doc and licensed under their terms.
        //TODO: what license is this under?
        String bucketName = "travel-sample";
        cluster.buckets().createBucket(BucketSettings.create(bucketName));
        // get a bucket reference
        Bucket bucket = cluster.bucket(bucketName);
        bucket.waitUntilReady(Duration.ofSeconds(120));
        bucket.collections().createScope("tenant_agent_00");
        bucket.collections().createCollection(CollectionSpec.create("users", "tenant_agent_00"));
        // get a user-defined collection reference
        Scope scope = bucket.scope("tenant_agent_00");
        Collection collection = scope.collection("users");

        // Upsert Document
        MutationResult upsertResult = collection.upsert("my-document", JsonObject.create().put("name", "mike"));
        Assertions.assertNotNull(upsertResult,
                "Upsert result is null and therefore the operation failed.");
        // Get Document
        GetResult getResult = collection.get("my-document");
        String name = getResult.contentAsObject().getString("name");
        Assertions.assertEquals("mike", name);
        // cleanup afterwards
        // TODO: maybe we need a @AfterEach method to clean up?
        cluster.buckets().dropBucket(bucketName);
    }
}
