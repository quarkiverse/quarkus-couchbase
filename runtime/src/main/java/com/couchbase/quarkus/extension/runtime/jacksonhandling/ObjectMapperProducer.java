package com.couchbase.quarkus.extension.runtime.jacksonhandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ObjectMapperProducer {
    @DefaultBean
    @Singleton
    @Produces
    public ObjectMapper objectMapper(@All List<ObjectMapperCustomizer> customizers,
            JacksonBuildTimeConfig jacksonBuildTimeConfig, JacksonSupport jacksonSupport) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ObjectMapperCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
        for (ObjectMapperCustomizer customizer : sortedCustomizers) {
            customizer.customize(objectMapper);
        }
        return objectMapper;
    }

    private List<ObjectMapperCustomizer> sortCustomizersInDescendingPriorityOrder(
            Iterable<ObjectMapperCustomizer> customizers) {
        List<ObjectMapperCustomizer> sortedCustomizers = new ArrayList<>();
        for (ObjectMapperCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        return sortedCustomizers;
    }
}
