package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.logic.CustomerLogic
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.openapi.RouterBuilder
import org.apache.logging.log4j.kotlin.Logging

class CustomerOpenApiRestVerticle(logic: CustomerLogic, private val mainRouter: Router) :
    AbstractCustomerRestVerticle(logic), Logging {

    // Some logic for this router is done in the start method so it is lateinit
    override lateinit var router: Router

    override fun start(startPromise: Promise<Void>) {
        RouterBuilder
            .create(vertx, "webroot/openapi.yaml")
            .onSuccess {
                it.securityHandler("customer_auth", buildAuthenticationHandler())
                it.operation("getCustomer").handler(this::loadCustomer) // GET
                it.operation("insertCustomer").handler(this::insertCustomer) // POST
                it.operation("deleteCustomer").handler(this::deleteCustomer) // DEL
                it.operation("updateCustomer").handler(this::updateCustomer) // PUT
                router = it.createRouter()
                super.start(startPromise) // Important to use the startPromise so lateinit of the router works
            }.onFailure {
                logger.error("Error when creating RouterBuilder", it)
                throw it
            }

    }

}