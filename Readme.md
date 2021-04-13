A Hello World for a simple REST API in VertX and Kotlin.

To launch tests:
```
./mvnw clean test
```

To package:
```
./mvnw clean package
```

To run:
```
./mvnw clean compile exec:java
```

Features:

- Simple validation and error handling
- Example operations for common HTTP operations in plain Vertx
- Example operations for common HTTP operations with OpenAPI
    - Also includes Swagger UI ready to try out the operations    
- Example file-based Basic Auth implementation with role validation
- Nice Kotlin data class handling
- Nice encapsulation of REST functionality and business logic
- Unit tests to test the exposed REST APIs
- Config handling for bootstrapping and during execution
- Major version of artifact is included in REST paths

ToDo:

- Proper asynchronous request handling via coroutines
- Maybe add a third way where OpenAPI documentation is created on the fly by annotations in the code
- Dependency Injection (maybe, quite a hassle)
- in-memory DB with nice persistence - this is a whole project on itself though
- Version setting does not work in the openapi.yaml file, maybe we should switch to https://www.mojohaus.org/build-helper-maven-plugin/parse-version-mojo.html 

Remarks:

- OpenAPI provides these features:
    - Routes and operations are defined and validated
    - Defining security (sadly roles are not supported by OpenAPI 3.0 for Basic Auth)
    - Schema validation of body parameters, query and path params
    - Validation of MIME types (however implementation does not return error code 415 if no handler accepts the request mime type but rather returns
      400 - did not bother creating a bug for that yet)
- Swagger UI is available on localhost:$MainVerticle.PORT/swagger/swagger-ui
- Role validation for basic auth is done horribly manually, I wonder if there is a nicer way
- To get the current config, see ConfigHandler.kt
- The major version in the REST path is set by the Maven Resources Plugin so check the pom.xml `<resources>` tag for details