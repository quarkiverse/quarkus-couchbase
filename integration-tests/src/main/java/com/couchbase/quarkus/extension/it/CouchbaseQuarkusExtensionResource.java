/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.couchbase.quarkus.extension.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.couchbase.client.java.Cluster;

@Path("/couchbase-quarkus-extension")
@ApplicationScoped
public class CouchbaseQuarkusExtensionResource {
    @Inject
    Cluster cluster;

    @GET
    @Path("/clusterCheck")
    public String clusterCheck() {
        var query = cluster.query("select 1 as test");
        return query.rowsAsObject().get(0).toString();
    }

    @GET
    @Path("/pingReport")
    public String clusterPing() {
        var ping = cluster.ping();
        return ping.exportToJson();
    }

    @GET
    @Path("/kvCheck")
    public String kvCheck() {
        var collection = cluster.bucket("default").scope("_default").collection("_default");
        collection.upsert("QuarkusTestDoc", "test success");
        var getResult = collection.get("QuarkusTestDoc");
        return getResult.toString();
    }

    @GET
    public String hello() {
        return "Hello couchbase-quarkus-extension";
    }
}
