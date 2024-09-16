package com.couchbase.quarkus.extension.runtime.nettyhandling.runtime.virtual;

public interface VirtualResponseHandler {
    void handleMessage(Object msg);

    void close();
}
