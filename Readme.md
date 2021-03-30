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
- Example file-based Basic Auth implementation
- Nice Kotlin data class handling
- Nice encapsulation of REST functionality and business logic
- Unit tests to test the exposed REST APIs

ToDo:

- Clean up what is a Verticle and what not...
- Asynchronous request handling / use coroutines
- Add an API versioning logic (MIME types and URL would be nice)
- Maybe add a third way where OpenAPI documentation is created on the fly by annotations in the code
- And maybe a fourth way with Jax RS :)
- Dependency Injection (maybe, quite a hassle)
- in memory DB with nice persistence - this should be a different project

Remarks:

- OpenAPI provides these features:
    - Routes and operations are defined and validated
    - Defining security (sadly roles are not supported by OpenAPI 3.0 and Basic Auth)
    - Schema validation of body parameters, query and path params
    - Validation of MIME types (however implementation does not return error code 415 if no handler accepts the request mime type but rather returns
      400 - did not bother creating a bug for that yet)
- Swagger UI is available on localhost:$MainVerticle.PORT/swagger/swagger-ui