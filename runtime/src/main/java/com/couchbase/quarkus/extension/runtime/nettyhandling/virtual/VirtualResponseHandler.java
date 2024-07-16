package com.couchbase.quarkus.extension.runtime.nettyhandling.virtual;

public interface VirtualResponseHandler {
    void handleMessage(Object msg);

    void close();
}
