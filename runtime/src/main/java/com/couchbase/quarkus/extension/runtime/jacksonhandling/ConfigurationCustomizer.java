package com.couchbase.quarkus.extension.runtime.jacksonhandling;

import java.time.ZoneId;
import java.util.TimeZone;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonInclude;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.DeserializationFeature;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.MapperFeature;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.SerializationFeature;

@Singleton
public class ConfigurationCustomizer implements ObjectMapperCustomizer {
    @Inject
    JacksonBuildTimeConfig jacksonBuildTimeConfig;

    @Inject
    JacksonSupport jacksonSupport;

    @Override
    public void customize(ObjectMapper objectMapper) {
        if (!jacksonBuildTimeConfig.failOnUnknownProperties) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (!jacksonBuildTimeConfig.failOnEmptyBeans) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        if (!jacksonBuildTimeConfig.writeDatesAsTimestamps) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (!jacksonBuildTimeConfig.writeDurationsAsTimestamps) {
            // this feature is enabled by default, so we disable it
            objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        }
        if (jacksonBuildTimeConfig.acceptCaseInsensitiveEnums) {
            objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        JsonInclude.Include serializationInclusion = jacksonBuildTimeConfig.serializationInclusion.orElse(null);
        if (serializationInclusion != null) {
            objectMapper.setSerializationInclusion(serializationInclusion);
        }
        ZoneId zoneId = jacksonBuildTimeConfig.timezone.orElse(null);
        if ((zoneId != null) && !zoneId.getId().equals("UTC")) { // Jackson uses UTC as the default, so let's not reset it
            objectMapper.setTimeZone(TimeZone.getTimeZone(zoneId));
        }
        if (jacksonSupport.configuredNamingStrategy().isPresent()) {
            objectMapper.setPropertyNamingStrategy(jacksonSupport.configuredNamingStrategy().get());
        }
    }

    @Override
    public int priority() {
        // we return the maximum possible priority to make sure these
        // settings are always applied first, before any other customizers.
        return ObjectMapperCustomizer.MAXIMUM_PRIORITY;
    }
}
