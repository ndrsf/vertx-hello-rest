package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.persistence.CustomerPersistence
import de.apwolf.vertx_rest.restadapter.CustomerOpenApiRestVerticle
import de.apwolf.vertx_rest.restadapter.CustomerRestVerticle
import de.apwolf.vertx_rest.restadapter.SwaggerUiVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import org.apache.logging.log4j.kotlin.Logging

class MainVerticle : AbstractVerticle(), Logging {

    private val customerLogic = CustomerLogic(CustomerPersistence())

    private lateinit var mainRouter: Router

    override fun start(startPromise: Promise<Void>) {
        mainRouter = Router.router(vertx)

        // We log error 500 because it gets returned when an exception got thrown somewhere in our code
        mainRouter.errorHandler(500) { handler -> logger.error("Error 500", handler.failure()) }

        vertx
            .createHttpServer()
            .requestHandler(mainRouter)
            .exceptionHandler { e -> logger.error("Exception in main Vertx", e) }
            .listen(8085)
            .onSuccess {
                CompositeFuture.all(
                    deploySwaggerUiVerticle(mainRouter),
                    deployCustomerRestVerticle(mainRouter, customerLogic),
                    deployCustomerOpenApiRestVerticle(mainRouter, customerLogic))
                    .onSuccess {
                        startPromise.complete()
                    }
                    .onFailure {
                        startPromise.fail(it)

                    }
            }
            .onFailure {
                logger.error("Exception in main Vertx", it)
                startPromise.fail(it)
            }
    }

    /**
     * Swagger API is available under http://localhost:8085/swagger/swagger-ui
     */
    private fun deploySwaggerUiVerticle(mainRouter: Router): Future<Void> {
        val swaggerUiVerticle = SwaggerUiVerticle()
        return Future.future { promise ->
            vertx.deployVerticle(swaggerUiVerticle)
                .onSuccess {
                    mainRouter.mountSubRouter("/swagger", swaggerUiVerticle.router)
                    logger.info("Deployed SwaggerUiVerticle")
                    promise.complete()
                }
                .onFailure {
                    logger.error("Failure when deploying SwaggerUiVerticle", it)
                    promise.fail(it)
                }
        }
    }

    private fun deployCustomerRestVerticle(mainRouter: Router, customerLogic: CustomerLogic): Future<Void> {
        val customerRestVerticle = CustomerRestVerticle(customerLogic)
        return Future.future { promise ->
            vertx.deployVerticle(customerRestVerticle)
                .onSuccess {
                    mainRouter.mountSubRouter("/customer", customerRestVerticle.router)
                    logger.info("Deployed CustomerRestVerticle")
                    promise.complete()
                }
                .onFailure {
                    logger.error("Failure when deploying CustomerRestVerticle", it)
                    promise.fail(it)
                }
        }
    }

    /**
     * Available at /customer2/customer
     */
    private fun deployCustomerOpenApiRestVerticle(mainRouter: Router, customerLogic: CustomerLogic): Future<Void> {
        val customerRestOpenApiVerticle = CustomerOpenApiRestVerticle(customerLogic, mainRouter)
        return Future.future { promise ->
            vertx.deployVerticle(customerRestOpenApiVerticle)
                .onSuccess {
                    // Remember to change the path in the openapi.yaml as well if you change the path
                    mainRouter.mountSubRouter("/customer2", customerRestOpenApiVerticle.router)
                    logger.info("Deployed CustomerOpenApiRestVerticle")
                    promise.complete()
                }
                .onFailure {
                    logger.error("Failure when deploying CustomerOpenApiRestVerticle", it)
                    promise.fail(it)
                }
        }
    }
}
