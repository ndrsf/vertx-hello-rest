package de.apwolf.vertx_rest

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MainVerticleTest {

    @Test
    fun testDeploy(vertx: Vertx, testContext: VertxTestContext) {
        vertx.exceptionHandler { testContext.failNow(it) }
        GlobalScope.launch(vertx.dispatcher()) {
            vertx.deployVerticle(MainVerticle()).await()
            // Wait 1 second to avoid annoying errors
            vertx.setTimer(1000) {
                testContext.completeNow()
            }
        }

    }
}