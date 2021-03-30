package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.persistence.CustomerPersistence
import de.apwolf.vertx_rest.restadapter.CustomerOpenApiRestVerticle
import de.apwolf.vertx_rest.restadapter.CustomerRestVerticle
import de.apwolf.vertx_rest.restadapter.SwaggerUiVerticle
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.apache.logging.log4j.kotlin.Logging

class MainVerticle : CoroutineVerticle(), Logging {

    companion object {

        const val PORT = 8085

    }

    override suspend fun start() {
        val mainRouter = Router.router(vertx)
        val customerLogic = CustomerLogic(CustomerPersistence())

        // We log error 500 because it gets returned when an exception got thrown somewhere in our code
        mainRouter.errorHandler(500) { handler -> logger.error("Error 500", handler.failure()) }

        vertx
            .createHttpServer()
            .requestHandler(mainRouter)
            .exceptionHandler { e -> logger.error("Exception in main Vertx", e) }
            .listen(PORT)
            .await() // Doesn't seem like we get race conditions if we don't wait, but who knows...

        // We don't wait for successful deployments here, exceptions bubble up and end the application
        deploySwaggerUiVerticle(mainRouter)
        deployCustomerRestVerticle(mainRouter, customerLogic)
        deployCustomerOpenApiRestVerticle(mainRouter, customerLogic)
        logger.info("Deployed MainVerticle")
    }

    /**
     * Swagger API is available under http://localhost:$port/swagger/swagger-ui
     */
    private fun deploySwaggerUiVerticle(mainRouter: Router) {
        val swaggerUiVerticle = SwaggerUiVerticle(mainRouter)
        vertx.deployVerticle(swaggerUiVerticle)
        logger.info("Deployed SwaggerUiVerticle")
    }

    private fun deployCustomerRestVerticle(mainRouter: Router, customerLogic: CustomerLogic) {
        val customerRestVerticle = CustomerRestVerticle(customerLogic, mainRouter)
        vertx.deployVerticle(customerRestVerticle)
        logger.info("Deployed CustomerRestVerticle")
    }

    /**
     * Available at /customer2/customer
     */
    private fun deployCustomerOpenApiRestVerticle(mainRouter: Router, customerLogic: CustomerLogic) {
        val customerRestOpenApiVerticle = CustomerOpenApiRestVerticle(customerLogic, mainRouter)
        vertx.deployVerticle(customerRestOpenApiVerticle)
        logger.info("Deployed CustomerOpenApiRestVerticle")
    }
}
