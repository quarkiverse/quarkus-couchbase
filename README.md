
<div align="center">
<img src="https://raw.githubusercontent.com/quarkiverse/quarkus-couchbase/master/docs/modules/ROOT/assets/images/quarkus.svg" width="67" height="70" >
<img src="https://raw.githubusercontent.com/quarkiverse/quarkus-couchbase/master/docs/modules/ROOT/assets/images/plus-sign.svg" height="70" >
<img src="https://raw.githubusercontent.com/quarkiverse/quarkus-couchbase/master/docs/modules/ROOT/assets/images/couchbase-filled.svg" height="70" >

# Quarkus Couchbase
</div>
<br>

<div align="center">

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.couchbase/quarkus-couchbase?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.couchbase/quarkus-couchbase)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![Build](https://github.com/quarkiverse/quarkus-couchbase/actions/workflows/build.yml/badge.svg)](https://github.com/quarkiverse/quarkus-couchbase/actions/workflows/build.yml)

</div>
Integrates Couchbase into Quarkus.

This extension is currently in beta status.  It supports:

- Dependency injecting a Couchbase `Cluster`.
- Configuring the Cluster through `application.properties`.  Currently, a minimal set of configuration options is provided.
- A dev service that starts a Couchbase server in a Docker container. With this you can develop your Quarkus app without having to install Couchbase on your machine.
- GraalVM/Mandrel/native-image.
- KV, Query, Transactions, Analytics, Search and Management operations.
- Micrometer metrics using `quarkus-micrometer`
- SmallRye Health checks (Readiness) using `quarkus-smallrye-health`

Please try it out and provide feedback, ideas and bug reports [on Github](https://github.com/quarkiverse/quarkus-couchbase/issues).

## Native Image
All main Cluster operations have been tested with our internal testing tools, however the extension remains in beta and some features may be missing or not be fully supported.

## Usage
Add it to your project:
```xml
<dependency>
  <groupId>io.quarkiverse.couchbase</groupId>
  <artifactId>quarkus-couchbase</artifactId>
  <version>1.0.0.Beta2</version>
</dependency>
```

Provide the Couchbase configuration in `application.properties`:
```properties
quarkus.couchbase.connection-string=localhost
quarkus.couchbase.username=username
quarkus.couchbase.password=password
```
To disable TestContainers, add:
```properties
quarkus.devservices.enabled=false
```

Now you can @Inject a Couchbase `Cluster` into your project:

```java
@Path("/couchbase")
public class TestCouchbaseResource {
    @Inject
    Cluster cluster;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/test")
    public String run() {
        // Get a reference to a particular Couchbase bucket and its default collection
        var bucket = cluster.bucket("travel-sample");
        var collection = bucket.defaultCollection() ;

        // Upsert a new document
        collection.upsert("test", JsonObject.create().put("foo", "bar"));

        // Fetch and print a document
        var doc = bucket.defaultCollection().get("test");
        System.out.println("Got doc " + doc.contentAsObject().toString());

        // Perform a N1QL query
        var queryResult = cluster.query("select * from `travel-sample` where url like 'http://marriot%' and country = 'United States';");

        queryResult.rowsAsObject().forEach(row -> {
            System.out.println(row.toString());
        });

        return "success!";
    }
}
```

And test http://localhost:8080/couchbase/test.

## Additional Configuration
Please refer to the [docs](https://github.com/quarkiverse/quarkus-couchbase/blob/main/docs/modules/ROOT/pages/configuration.adoc) for additional configuration options.

## Limitations
In this a beta release the Cluster configuration options are limited to the three shown above.  
This means that a Couchbase cluster configured securely and requiring TLS or a client or server certificate, cannot currently be connected to.

## License
All the files under `nettyhandling` directories, both in the `runtime` and `deployment` modules are
taken as-were or modified from the official [netty](https://github.com/quarkusio/quarkus/tree/main/extensions/netty) extension.
Couchbase does not intend copyright infringement or claim ownership over these files or their content.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://programmatix.github.io/Words/projects"><img src="https://avatars.githubusercontent.com/u/795437?v=4?s=100" width="100px;" alt="Graham Pople"/><br /><sub><b>Graham Pople</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-couchbase/commits?author=programmatix" title="Code">💻</a> <a href="#maintenance-programmatix" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/emilienbev"><img src="https://avatars.githubusercontent.com/u/44171454?v=4?s=100" width="100px;" alt="Emilien Bevierre"/><br /><sub><b>Emilien Bevierre</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-couchbase/commits?author=emilienbev" title="Code">💻</a> <a href="#maintenance-emilienbev" title="Maintenance">🚧</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
