package com.couchbase.quarkus.extension.runtime;

import com.couchbase.client.core.encryption.CryptoManager;
import com.couchbase.client.java.codec.DefaultJsonSerializer;
import com.couchbase.client.java.codec.JsonSerializer;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.couchbase.client.java.env.ClusterEnvironment")
final class Target_ClusterEnvironment_newDefaultSerializer {

    @Substitute
    private JsonSerializer newDefaultSerializer(CryptoManager cryptoManager) {
        return DefaultJsonSerializer.create(cryptoManager);
    }
}
