package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.logic.CustomerLogic
import de.apwolf.vertx_rest.persistence.CustomerPersistence
import de.apwolf.vertx_rest.restadapter.CustomerOpenApiRestVerticle
import de.apwolf.vertx_rest.restadapter.CustomerRestVerticle
import de.apwolf.vertx_rest.restadapter.SwaggerUiVerticle
import de.apwolf.vertx_rest.util.ConfigHandler
import de.apwolf.vertx_rest.util.GET_CURRENT_CONFIG
import de.apwolf.vertx_rest.util.HTTP_PORT
import de.apwolf.vertx_rest.util.REST_VERSION
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import org.apache.logging.log4j.kotlin.Logging
import java.lang.module.ModuleDescriptor

class MainVerticle : CoroutineVerticle(), Logging {

    companion object {

        var PORT = 0

        var REST_PATH_VERSION = ""

    }

    override suspend fun start() {
        deployConfigHandler().await() // Deploy ConfigHandler at first so we can read our config params for startup
        val config = awaitResult<Message<JsonObject>> { vertx.eventBus().request(GET_CURRENT_CONFIG, "", it) }
        PORT = config.body().getInteger(HTTP_PORT)
        REST_PATH_VERSION = buildRestPathVersion(config.body())

        val customerLogic = CustomerLogic(CustomerPersistence())

        val mainRouter = Router.router(vertx)
        // We log error 500 because it gets returned when an exception got thrown somewhere in our code
        mainRouter.errorHandler(500) { handler -> logger.error("Error 500", handler.failure()) }
        // Our main route is under the major version
        val mainRoute = mainRouter.route("/$REST_PATH_VERSION/*")
        // You can only have one subRouter under a route in Vertx, so we need this intermediate router
        val versionedRouter = Router.router(vertx)
        mainRoute.subRouter(versionedRouter)

        vertx
            .createHttpServer()
            .requestHandler(mainRouter)
            .exceptionHandler { e -> logger.error("Exception in main Vertx", e) }
            .listen(PORT)
            .await() // Doesn't seem like we get race conditions if we don't wait, but who knows...
        logger.info("Started webserver on localhost:$PORT/$REST_PATH_VERSION")

        // We don't wait for successful deployments here, exception handling is in the methods themselves
        // TODO think of a nice way to fail deployment if a subdeployment fails - currently we use the exceptionHandler
        deploySwaggerUiVerticle(versionedRouter)
        deployCustomerRestVerticle(versionedRouter, customerLogic)
        deployCustomerOpenApiRestVerticle(versionedRouter, customerLogic)
        logger.info("Deployed MainVerticle")
    }

    /**
     * Beautiful code... we read the property, make sure it is a valid version string (done by ModuleDescriptor.Version)
     * and then take the first number which is the major version
     *
     * @return "v$majorVersion"
     */
    fun buildRestPathVersion(config: JsonObject): String {
        return "v" + ModuleDescriptor
            .Version.parse(config.getString(REST_VERSION))
            .toString().subSequence(0, 1).toString()

    }

    private fun deployConfigHandler(): Future<String> {
        val configHandlerVerticle = ConfigHandler()
        return vertx.deployVerticle(configHandlerVerticle)
    }

    // TODO http://localhost:8085/swagger/swagger-ui leads to http://localhost:8085/swagger/swagger-ui/index.html/
    private fun deploySwaggerUiVerticle(mainRouter: Router) {
        val swaggerUiVerticle = SwaggerUiVerticle(mainRouter)
        vertx.deployVerticle(swaggerUiVerticle).onFailure { throw it }
        logger.info("Deployed SwaggerUiVerticle")
    }

    /**
     * Available at /customer
     */
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
