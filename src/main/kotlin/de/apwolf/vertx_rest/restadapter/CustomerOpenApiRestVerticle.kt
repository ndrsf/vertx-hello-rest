package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.logic.CustomerLogic
import io.vertx.ext.web.Router
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.kotlin.coroutines.await
import org.apache.logging.log4j.kotlin.Logging

class CustomerOpenApiRestVerticle(logic: CustomerLogic, private val mainRouter: Router) :
    AbstractCustomerRestVerticle(logic), Logging {

    override suspend fun start() {
        val builder = RouterBuilder.create(vertx, "webroot/openapi.yaml").await()

        builder.securityHandler("customer_auth", buildAuthenticationHandler())
        builder.operation("getCustomer").handler(this::loadCustomer) // GET
        builder.operation("insertCustomer").handler(this::insertCustomer) // POST
        builder.operation("deleteCustomer").handler(this::deleteCustomer) // DEL
        builder.operation("updateCustomer").handler(this::updateCustomer) // PUT

        // Remember to change the path in the openapi.yaml as well if you change the path
        mainRouter.mountSubRouter("/customer2", builder.createRouter())

    }

}