package de.apwolf.vertx_rest.restadapter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import de.apwolf.vertx_rest.logic.CustomerId
import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.logic.CustomerLogicRequestMode
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.ext.auth.properties.PropertyFileAuthentication
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BasicAuthHandler
import kotlin.reflect.KClass
import org.apache.logging.log4j.kotlin.Logging

abstract class AbstractCustomerRestVerticle(private val logic: CustomerLogic) : AbstractVerticle(), Logging {

    companion object {

        // We use a custom mapper to allow Kotlin data mapping. We don't want io.vertx.core.json.Json because
        // it cannot handle Kotlin data classes
        val mapper: ObjectMapper = JsonMapper.builder().build()

        internal fun <T : Any> fromJsonToObject(json: String, type: KClass<T>): T {
            configureJacksonObjectMapper(mapper)
            return mapper.readValue(json, type.java)
        }
    }

    // Visible router so we can manage routing from outside
    abstract val router: Router

    internal fun loadCustomer(routingContext: RoutingContext) {
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

    internal fun insertCustomer(routingContext: RoutingContext) {
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

    internal fun updateCustomer(routingContext: RoutingContext) {
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

    internal fun deleteCustomer(routingContext: RoutingContext) {
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
        val authProvider = PropertyFileAuthentication.create(vertx, "basic-auth.properties")
        return BasicAuthHandler.create(authProvider)
    }

    private fun buildInvalidCustomerIdResponse(customerId: String,
        routingContext: RoutingContext): HttpServerResponse {
        return routingContext
            .response()
            .setStatusCode(400)
            .setStatusMessage("customerId $customerId is no valid Integer")
    }

}