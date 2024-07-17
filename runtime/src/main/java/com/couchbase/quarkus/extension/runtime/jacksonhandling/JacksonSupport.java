package com.couchbase.quarkus.extension.runtime.jacksonhandling;

import java.util.Optional;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.PropertyNamingStrategies;

public interface JacksonSupport {

    Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy();
}
