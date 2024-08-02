package com.couchbase.quarkus.extension.runtime.jacksonhandling;

import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.BufferRecycler;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.util.RecyclerPool;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.jackson.HybridJacksonPool;

public class VertxHybridPoolObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        var existingMapperPool = objectMapper.getFactory()._getRecyclerPool();
        // if the recycler pool in use is the default jackson one it means that user hasn't
        // explicitly chosen any, so we can replace it with the vert.x virtual thread friendly one
        if (existingMapperPool.getClass() == JsonRecyclerPools.defaultPool().getClass()) {
            objectMapper.getFactory().setRecyclerPool((RecyclerPool<BufferRecycler>) HybridJacksonPool.getInstance());
        }
    }

    @Override
    public int priority() {
        return ObjectMapperCustomizer.super.priority();
    }

    @Override
    public int compareTo(ObjectMapperCustomizer o) {
        return ObjectMapperCustomizer.super.compareTo(o);
    }

}
