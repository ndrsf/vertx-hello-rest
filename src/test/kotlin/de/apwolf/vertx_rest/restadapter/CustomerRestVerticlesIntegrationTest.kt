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
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
            vertx.exceptionHandler { testContext.failNow(it) }
            vertx.deployVerticle(MainVerticle()).onSuccess {
                testContext.completeNow()
            }.onFailure {
                testContext.failNow(it)
            }

        }
    }

    private lateinit var webRequest: HttpRequest<Buffer>

    private var baseUrl = "/customer2/customer"

    @Test
    internal fun testCustomerRestService(vertx: Vertx, testContext: VertxTestContext) {
        baseUrl = "/customer"
        testInsertCustomer()
            .onSuccess { customerId -> testGetCustomer(customerId) }
            .onSuccess { customerId -> testUpdateCustomer(customerId) }
            .onSuccess { customerId -> testDeleteCustomer(customerId) }
            .onSuccess {
                baseUrl = "/customer2/customer"
                // Vertx closes http connections if we don't add a delay - not a big problem but prints errors
                // if we don't wait for a second
                vertx.setTimer(1000) {
                    testInsertCustomer()
                        .onSuccess { customerId -> testGetCustomer(customerId) }
                        .onSuccess { customerId -> testUpdateCustomer(customerId) }
                        .onSuccess { customerId -> testDeleteCustomer(customerId) }
                        .onSuccess { vertx.setTimer(1000) { testContext.completeNow() } }
                }
            }

    }

    private fun testInsertCustomer(): Future<Int> {
        return Future.future { promise ->
            configureWebClient(HttpMethod.POST, baseUrl)
            val request = CustomerRequest(null,
                insertedCustomerName,
                insertedCustomerBirthday)
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
            val request = CustomerRequest(null,
                updatedCustomerName,
                updatedCustomerBirthday)
            webRequest.sendJson(request)
                .onSuccess {
                    val response =
                        AbstractCustomerRestVerticle.fromJsonToObject(it.bodyAsString(), CustomerResponse::class)
                    assertEquals(updatedCustomerName, response.name)
                    assertEquals(updatedCustomerBirthday, response.birthday)
                    assertEquals(customerId, response.id)
                    promise.complete()
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

    private fun configureWebClient(httpMethod: HttpMethod, url: String) {
        webRequest = WebClient.create(vertx)
            .request(httpMethod, MainVerticle.PORT, "localhost", url)
            .authentication(UsernamePasswordCredentials("writer", "writerpw"))
            .expect(ResponsePredicate.SC_SUCCESS)
    }
}