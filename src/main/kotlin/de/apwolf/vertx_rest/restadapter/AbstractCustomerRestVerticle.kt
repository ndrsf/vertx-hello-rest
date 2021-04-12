package de.apwolf.vertx_rest.restadapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import de.apwolf.vertx_rest.logic.CustomerId
import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.logic.CustomerLogicRequestMode
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.ext.auth.authorization.AuthorizationProvider
import io.vertx.ext.auth.authorization.RoleBasedAuthorization
import io.vertx.ext.auth.properties.PropertyFileAuthentication
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.Logging
import java.util.stream.Collectors
import kotlin.reflect.KClass

abstract class AbstractCustomerRestVerticle(private val logic: CustomerLogic) : CoroutineVerticle(), Logging {

    // We need the auth to check for the roles ourselves, lateinit doesn't work in Kotlin coroutines
    var authProvider: PropertyFileAuthentication? = null

    companion object {

        const val READ_ROLE = "read-customer"
        const val WRITE_ROLE = "write-customer"

        const val BASIC_AUTH_FILE = "basic-auth.properties"

        // We use a custom mapper to allow Kotlin data mapping. We don't want io.vertx.core.json.Json because
        // it cannot handle Kotlin data classes
        val mapper: ObjectMapper = JsonMapper.builder().build()

        internal fun <T : Any> fromJsonToObject(json: String, type: KClass<T>): T {
            configureJacksonObjectMapper(mapper)
            return mapper.readValue(json, type.java)
        }
    }

    internal suspend fun loadCustomer(routingContext: RoutingContext) {
        if (!checkRoles(routingContext, listOf(READ_ROLE))) {
            buildInsufficientRightsResponse(routingContext).end()
            return
        }
        val requestCustomerId: String = routingContext.request().getParam("customerId")
        logger.info("Loading customer data for customer $requestCustomerId")

        val customerId = CustomerId.of(requestCustomerId)

        if (customerId == null) {
            buildInvalidCustomerIdResponse(requestCustomerId, routingContext).end()
            return
        }

        val customer = logic.loadCustomer(customerId)
        if (customer != null) {
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .putHeader("charset", "utf-8")
                .end(Json.encodePrettily(CustomerResponse.fromCustomer(customer)))
            return
        } else {
            routingContext
                .response()
                .setStatusCode(404)
                .setStatusMessage("Customer $requestCustomerId not found").end()
            return
        }

    }

    internal suspend fun insertCustomer(routingContext: RoutingContext) {
        if (!checkRoles(routingContext, listOf(WRITE_ROLE, READ_ROLE))) {
            buildInsufficientRightsResponse(routingContext).end()
            return
        }
        val requestCustomer: CustomerRequest = fromJsonToObject(routingContext.bodyAsString, CustomerRequest::class)

        val updatedCustomer =
            logic.insertCustomer(requestCustomer.toCustomerLogicRequest(CustomerLogicRequestMode.INSERT))
        logger.info("Inserted customer $updatedCustomer")

        routingContext
            .response()
            .putHeader("content-type", "application/json")
            .putHeader("charset", "utf-8")
            .end(Json.encodePrettily(CustomerResponse.fromCustomer(updatedCustomer)))
    }

    internal suspend fun updateCustomer(routingContext: RoutingContext) {
        if (!checkRoles(routingContext, listOf(WRITE_ROLE, READ_ROLE))) {
            buildInsufficientRightsResponse(routingContext).end()
            return
        }
        val requestCustomerId = routingContext.request().getParam("customerId")

        val customerId = CustomerId.of(requestCustomerId)

        if (customerId == null) {
            buildInvalidCustomerIdResponse(requestCustomerId, routingContext).end()
            return
        }

        val requestCustomer: CustomerRequest = fromJsonToObject(routingContext.bodyAsString, CustomerRequest::class)
        requestCustomer.id = customerId.id // we must take our own id and not rely on the sent id
        val updatedCustomer =
            logic.updateCustomer(requestCustomer.toCustomerLogicRequest(CustomerLogicRequestMode.UPDATE))
        logger.info("Updated customer $customerId to $requestCustomer")

        routingContext
            .response()
            .putHeader("content-type", "application/json")
            .putHeader("charset", "utf-8")
            .end(Json.encodePrettily(CustomerResponse.fromCustomer(updatedCustomer)))
    }

    internal suspend fun deleteCustomer(routingContext: RoutingContext) {
        if (!checkRoles(routingContext, listOf(WRITE_ROLE))) {
            buildInsufficientRightsResponse(routingContext).end()
            return
        }
        val requestCustomerId: String = routingContext.request().getParam("customerId")
        logger.info("Deleting customer data for customer $requestCustomerId")

        val customerId = CustomerId.of(requestCustomerId)

        if (customerId == null) {
            buildInvalidCustomerIdResponse(requestCustomerId, routingContext).end()
            return
        }

        val deletedCustomer = logic.deleteCustomer(customerId)
        if (deletedCustomer) {
            val logMessage = "Customer $customerId was deleted"
            logger.info(logMessage)

            routingContext.response().setStatusMessage(logMessage)
                .setStatusCode(204)
                .end()
            return
        }
        val logMessage = "Customer $customerId was not found and therefore not deleted"
        logger.info(logMessage)

        routingContext.response().setStatusMessage(logMessage)
            .setStatusCode(404)
            .end()
        return
    }

    /**
     * OpenAPI complains if auth handler is not set via RouterBuilder, but at least we can encapsulate the
     * AuthenticationHandler creation process
     */
    internal fun buildAuthenticationHandler(): AuthenticationHandler {
        authProvider = PropertyFileAuthentication.create(vertx, BASIC_AUTH_FILE)
        return BasicAuthHandler.create(authProvider)
    }

    /**
     * Shamelessly stolen from https://medium.com/hackernoon/asynchronous-temporal-rest-with-vert-x-keycloak-and-kotlin-coroutines-217b25756314
     * TODO use executeBlockingAwait?
     */
    internal fun coroutineHandler(fn: suspend (RoutingContext) -> Unit): Handler<RoutingContext> {
        return Handler<RoutingContext> { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    fn(ctx)
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        }
    }

    /**
     * Checks if the user has the required roles. Check for existence of user and correct password is done in the
     * BasicAuthHandler. I really hope there is a nicer way in Vertx to check for roles in basic auth, but I don't
     * find any...
     *
     * @return true if the user has all required roles, otherwise returns false
     *
     */
    private suspend fun checkRoles(routingContext: RoutingContext, requiredRoles: List<String>): Boolean {
        val finalAuthProvider = authProvider // to allow smart cast
        if (finalAuthProvider is AuthorizationProvider) {
            // The following sets the authorizations for the current user... yeah, I don't like it either...
            finalAuthProvider.getAuthorizations(routingContext.user()).await()
            // Now we have to filter out the roles
            val userRoles = routingContext.user().authorizations().get(BASIC_AUTH_FILE)
                .stream()
                .filter { it is RoleBasedAuthorization }
                .map { it as RoleBasedAuthorization }
                .map { it.role }
                .collect(Collectors.toList())

            return userRoles.containsAll(requiredRoles)
        }
        logger.error("Used auth provider is not of expected type, could not check for roles!")
        return false
    }

    /**
     * You have to call end() afterwards yourself, in case you want to add something to the response
     */
    private fun buildInsufficientRightsResponse(routingContext: RoutingContext): HttpServerResponse {
        return routingContext
            .response()
            .setStatusCode(403)
            .setStatusMessage("User does not have required roles")
    }

    /**
     * You have to call end() afterwards yourself, in case you want to add something to the response
     */
    private fun buildInvalidCustomerIdResponse(customerId: String, routingContext: RoutingContext): HttpServerResponse {
        return routingContext
            .response()
            .setStatusCode(400)
            .setStatusMessage("customerId $customerId is no valid Integer")
    }

}