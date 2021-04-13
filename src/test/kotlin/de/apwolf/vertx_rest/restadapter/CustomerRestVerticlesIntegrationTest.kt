package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.MainVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

/**
 * One test to rule them all - this test deploys our main verticle and calls the rest APIs of CustomerRestVerticle
 * and CustomerOpenApiRestVerticle.
 */
@ExtendWith(VertxExtension::class)
internal class CustomerRestVerticlesIntegrationTest {

    companion object {

        const val insertedCustomerName = "Test User"
        val insertedCustomerBirthday: LocalDate = LocalDate.of(2020, 12, 31)

        const val updatedCustomerName = "Updated Test"
        val updatedCustomerBirthday: LocalDate = LocalDate.of(2021, 12, 31)

        lateinit var vertx: Vertx

        @Suppress("unused") // IntelliJ cannot cope with beforeAll
        @BeforeAll
        @JvmStatic
        fun beforeAll(vertx: Vertx, testContext: VertxTestContext) {
            configureVertxDefaultJacksonMapper()

            this.vertx = vertx
            vertx.exceptionHandler {
                testContext.failNow(it)
            }
            GlobalScope.launch(vertx.dispatcher()) {
                vertx.deployVerticle(MainVerticle())
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
    }

    private lateinit var webRequest: HttpRequest<Buffer>

    private var baseUrl = ""

    @Test
    internal fun testCustomerOpenApiRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        baseUrl = "/${MainVerticle.REST_PATH_VERSION}/customer2/customer"
        testInsertCustomer()
            .compose { customerId -> testGetCustomer(customerId) }
            .compose { customerId -> testUpdateCustomer(customerId) }
            .compose { customerId -> testDeleteCustomer(customerId) }
            .onFailure { testContext.failNow(it) } // will be called if any of the methods fail
            .onSuccess { testContext.completeNow() } // will be called if all of the methods succeed

    }

    @Test
    internal fun testCustomerRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        baseUrl = "/${MainVerticle.REST_PATH_VERSION}/customer"
        testInsertCustomer()
            .compose { customerId -> testGetCustomer(customerId) }
            .compose { customerId -> testUpdateCustomer(customerId) }
            .compose { customerId -> testDeleteCustomer(customerId) }
            .onFailure { testContext.failNow(it) } // will be called if any of the methods fail
            .onSuccess { testContext.completeNow() } // will be called if all of the methods succeed
    }

    @Test
    internal fun testInvalidUserForCustomerRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        configureWebClient(
            HttpMethod.GET, "/${MainVerticle.REST_PATH_VERSION}/customer/1",
            UsernamePasswordCredentials("retirw", "writerpw"),
            ResponsePredicate.status(401)
        )
        webRequest.send()
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    @Test
    internal fun testInvalidUserForCustomerOpenApiRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        configureWebClient(
            HttpMethod.GET, "/${MainVerticle.REST_PATH_VERSION}/customer2/customer/1",
            UsernamePasswordCredentials("retirw", "writerpw"),
            ResponsePredicate.status(401)
        )
        webRequest.send()
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    @Test
    internal fun testWrongUserForCustomerRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        configureWebClient(
            HttpMethod.GET, "/${MainVerticle.REST_PATH_VERSION}/customer/1",
            UsernamePasswordCredentials("writer", "writerpw"),
            ResponsePredicate.status(403)
        )
        webRequest.send()
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    @Test
    internal fun testWrongUserForCustomerOpenApiRestVerticle(vertx: Vertx, testContext: VertxTestContext) {
        configureWebClient(
            HttpMethod.GET, "/${MainVerticle.REST_PATH_VERSION}/customer2/customer/1",
            UsernamePasswordCredentials("writer", "writerpw"),
            ResponsePredicate.status(403)
        )
        webRequest.send()
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    private fun testInsertCustomer(): Future<Int> {
        return Future.future { promise ->
            configureWebClient(HttpMethod.POST, baseUrl)
            val request = CustomerRequest(
                null,
                insertedCustomerName,
                insertedCustomerBirthday
            )
            webRequest.sendJson(request)
                .onSuccess {
                    val response =
                        AbstractCustomerRestVerticle.fromJsonToObject(it.bodyAsString(), CustomerResponse::class)
                    assertEquals(insertedCustomerName, response.name)
                    assertEquals(insertedCustomerBirthday, response.birthday)
                    assertNotNull(response.id)
                    promise.complete(response.id)
                }
                .onFailure {
                    promise.fail(it)
                }
        }
    }

    private fun testGetCustomer(customerId: Int): Future<Int> {
        return Future.future { promise ->
            configureWebClient(HttpMethod.GET, "$baseUrl/${customerId}")
            webRequest.send().onSuccess {
                val response = AbstractCustomerRestVerticle.fromJsonToObject(it.bodyAsString(), CustomerResponse::class)
                assertEquals(insertedCustomerName, response.name)
                assertEquals(insertedCustomerBirthday, response.birthday)
                assertEquals(customerId, response.id)
                promise.complete(customerId)
            }.onFailure {
                promise.fail(it)
            }
        }

    }

    private fun testUpdateCustomer(customerId: Int): Future<Int> {
        return Future.future { promise ->
            configureWebClient(HttpMethod.PUT, "$baseUrl/${customerId}")
            val request = CustomerRequest(
                null,
                updatedCustomerName,
                updatedCustomerBirthday
            )
            webRequest.sendJson(request)
                .onSuccess {
                    val response =
                        AbstractCustomerRestVerticle.fromJsonToObject(it.bodyAsString(), CustomerResponse::class)
                    assertEquals(updatedCustomerName, response.name)
                    assertEquals(updatedCustomerBirthday, response.birthday)
                    assertEquals(customerId, response.id)
                    promise.complete(customerId)
                }
                .onFailure {
                    promise.fail(it)
                }
        }
    }

    private fun testDeleteCustomer(customerId: Int): Future<Int> {
        return Future.future { promise ->
            configureWebClient(HttpMethod.DELETE, "$baseUrl/${customerId}")
            webRequest.send().onSuccess {
                promise.complete(customerId)
            }.onFailure {
                promise.fail(it)
            }
        }
    }

    private fun configureWebClient(
        httpMethod: HttpMethod, url: String,
        credentials: UsernamePasswordCredentials = UsernamePasswordCredentials("both", "bothpw"),
        expectedStatus: ResponsePredicate = ResponsePredicate.SC_SUCCESS
    ) {
        webRequest = WebClient.create(vertx)
            .request(httpMethod, MainVerticle.PORT, "localhost", url)
            .authentication(credentials)
            .expect(expectedStatus)
    }
}