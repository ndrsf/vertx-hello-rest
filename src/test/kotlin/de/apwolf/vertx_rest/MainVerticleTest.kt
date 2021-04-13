package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.restadapter.configureVertxDefaultJacksonMapper
import de.apwolf.vertx_rest.util.REST_VERSION
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MainVerticleTest {

    companion object {

        private lateinit var vertx: Vertx

        private lateinit var sut: MainVerticle

        @Suppress("unused") // IntelliJ cannot cope with beforeAll
        @BeforeAll
        @JvmStatic
        fun beforeAll(vertx: Vertx, testContext: VertxTestContext) {
            configureVertxDefaultJacksonMapper()

            this.vertx = vertx
            this.sut = MainVerticle()
            vertx.exceptionHandler {
                testContext.failNow(it)
            }
            GlobalScope.launch(vertx.dispatcher()) {
                vertx.deployVerticle(sut)
                    .onSuccess {
                        // Horrible hack, onsuccess is faster than the exception handler, so if a verticle deployment
                        // fails the test is still green, so we add a delay
                        vertx.setTimer(100) { testContext.completeNow() }
                    }
                    .onFailure {
                        testContext.failNow(it)
                    }

            }
        }


        @Suppress("unused") // ... or afterAll
        @AfterAll
        @JvmStatic
        fun afterAll(vertx: Vertx, testContext: VertxTestContext) {
            // Give Netty some time
            vertx.setTimer(1000) { testContext.completeNow() }
        }
    }

    @Test
    fun testDeploy(vertx: Vertx, testContext: VertxTestContext) {
        // Wait 1 second and then it seems like we deployed :^)
        vertx.setTimer(1000) {
            testContext.completeNow()
        }
    }

    @Test
    fun testSetRestPathVersion(vertx: Vertx, testContext: VertxTestContext) {
        assertEquals(1, sut.buildRestPathVersion(JsonObject().put(REST_VERSION, "1.0.0")))
        assertEquals(1, sut.buildRestPathVersion(JsonObject().put(REST_VERSION, "1.0.0-SNAPSHOT")))
        assertEquals(2, sut.buildRestPathVersion(JsonObject().put(REST_VERSION, "2.13.15.17.19-SNAPSHOT")))
        assertEquals(2, sut.buildRestPathVersion(JsonObject().put(REST_VERSION, "2.13.15.17.19")))

        testContext.completeNow()
    }

    @Test
    fun testSetRestPathVersion_invalidString(vertx: Vertx, testContext: VertxTestContext) {
        assertThrows<IllegalArgumentException> {
            sut.buildRestPathVersion(JsonObject().put(REST_VERSION, "whoops"))
        }

        testContext.completeNow()
    }

    @Test
    fun testSetRestPathVersion_nullString(vertx: Vertx, testContext: VertxTestContext) {
        assertThrows<IllegalArgumentException> {
            sut.buildRestPathVersion(JsonObject())
        }

        testContext.completeNow()
    }
}