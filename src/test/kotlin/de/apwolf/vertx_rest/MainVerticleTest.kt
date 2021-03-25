package de.apwolf.vertx_rest

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MainVerticleTest {

    @Test
    fun testDeploy(vertx: Vertx, testContext: VertxTestContext) {
        vertx.deployVerticle(MainVerticle()).onSuccess {
            testContext.completeNow()
        }
            .onFailure {
                testContext.failNow(it)
            }
    }
}