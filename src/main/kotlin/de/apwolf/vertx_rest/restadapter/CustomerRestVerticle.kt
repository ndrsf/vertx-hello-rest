package de.apwolf.vertx_rest.restadapter

import de.apwolf.vertx_rest.logic.CustomerLogic
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.apache.logging.log4j.kotlin.Logging

class CustomerRestVerticle(logic: CustomerLogic, private val mainRouter: Router) : Logging,
    AbstractCustomerRestVerticle(logic) {

    override suspend fun start() {
        val router = Router.router(vertx)
        router.route("/*")
            .handler(BodyHandler.create()) // allow bodies for everything because why not :o)
            .handler(buildAuthenticationHandler()) // secure all routes with basic auth
        router
            .get("/:customerId")
            .handler(this::loadCustomer)
        router
            .put("/:customerId")
            .consumes("application/json") // sadly there doesn't seem to be a way to define accepted mime types per route
            .handler { routingContext -> updateCustomer(routingContext) }
        router
            .post("/")
            .consumes("application/json")
            .handler { routingContext -> insertCustomer(routingContext) }
        router
            .delete("/:customerId")
            .handler(this::deleteCustomer)

        mainRouter.mountSubRouter("/customer", router)
    }

}