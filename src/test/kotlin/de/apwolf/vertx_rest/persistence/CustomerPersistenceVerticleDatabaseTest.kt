package de.apwolf.vertx_rest.persistence

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class CustomerPersistenceVerticleDatabaseTest {

    @Test
    fun test(vertx: Vertx, testContext: VertxTestContext) {
        vertx.deployVerticle(CustomerPersistenceVerticle())
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }
}