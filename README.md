# Quarkus Couchbase Extension
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
Integrates Couchbase into Quarkus.

This extension is currently in alpha status.  It supports:

- Dependency injecting a Couchbase `Cluster`.
- Configuring the Cluster through `application.properties`.  Currently a minimal set of configuration options is provided.
- Graal/native-image, though so far it has been minimally tested with basic cases.

Please try it out and provide feedback, ideas and bug reports [on Github](https://github.com/quarkiverse/quarkus-couchbase/issues).

## Usage
Add it to your project:
```
<dependency>
  <groupId>com.couchbase</groupId>
  <artifactId>quarkus-couchbase</artifactId>
  <version>1.0.0-alpha.1</version>
</dependency>
```

(Note this will soon be updated to the new groupId of io.quarkiverse.couchbase, but it has not yet been published to Maven under this.)

Provide the Couchbase configuration in `application.properties`:
```
quarkus.couchbase.connection-string=localhost
quarkus.couchbase.username=username
quarkus.couchbase.password=password
```

Now you can @Inject a Couchbase `Cluster` into your project:

```
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

## Limitations
In this early alpha release the configuration options are limited to the three shown above.  
This means that a Couchbase cluster configured securely and requiring TLS or a client or server certificate, cannot currently be connected to.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://programmatix.github.io/Words/projects"><img src="https://avatars.githubusercontent.com/u/795437?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Graham Pople</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-couchbase/commits?author=programmatix" title="Code">💻</a> <a href="#maintenance-programmatix" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!