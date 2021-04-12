package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.persistence.CustomerPersistence
import de.apwolf.vertx_rest.restadapter.CustomerOpenApiRestVerticle
import de.apwolf.vertx_rest.restadapter.CustomerRestVerticle
import de.apwolf.vertx_rest.restadapter.SwaggerUiVerticle
import de.apwolf.vertx_rest.util.ConfigHandler
import de.apwolf.vertx_rest.util.GET_CURRENT_CONFIG
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
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

        // We don't wait for successful deployments here, exception handling is in the methods themselves
        // TODO think of a nice way to fail deployment if a subdeployment fails - currently we use the exceptionHandler
        deploySwaggerUiVerticle(mainRouter)
        deployCustomerRestVerticle(mainRouter, customerLogic)
        deployCustomerOpenApiRestVerticle(mainRouter, customerLogic)
        deployConfigHandler()
        logger.info("Deployed MainVerticle")
    }

    private fun deployConfigHandler() {
        val configHandlerVerticle = ConfigHandler()
        vertx.deployVerticle(configHandlerVerticle).onFailure { throw it }
        logger.info("Deployed ConfigHandler")
    }

    /**
     * Swagger API is available under http://localhost:$port/swagger/swagger-ui
     */
    private fun deploySwaggerUiVerticle(mainRouter: Router) {
        val swaggerUiVerticle = SwaggerUiVerticle(mainRouter)
        vertx.deployVerticle(swaggerUiVerticle).onFailure { throw it }
        logger.info("Deployed SwaggerUiVerticle")
    }

    private fun deployCustomerRestVerticle(mainRouter: Router, customerLogic: CustomerLogic) {
        val customerRestVerticle = CustomerRestVerticle(customerLogic, mainRouter)
        vertx.deployVerticle(customerRestVerticle).onFailure { throw it }
        logger.info("Deployed CustomerRestVerticle")
    }

    /**
     * Available at /customer2/customer
     */
    private fun deployCustomerOpenApiRestVerticle(mainRouter: Router, customerLogic: CustomerLogic) {
        val customerRestOpenApiVerticle = CustomerOpenApiRestVerticle(customerLogic, mainRouter)
        vertx.deployVerticle(customerRestOpenApiVerticle).onFailure { throw it }
        logger.info("Deployed CustomerOpenApiRestVerticle")
    }
}
