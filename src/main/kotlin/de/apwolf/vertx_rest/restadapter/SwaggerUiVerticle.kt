package de.apwolf.vertx_rest.restadapter

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.apache.logging.log4j.kotlin.Logging

/**
 * Swagger UI is available on localhost:8085/swagger/swagger-ui
 *
 * I would really like to make the path to the UI easier (just /swagger) but I can't seem to get it working.
 */
class SwaggerUiVerticle(private val mainRouter: Router) : CoroutineVerticle(), Logging {

    override suspend fun start() {
        val router = Router.router(vertx)
        router
            .route("/*")
            // Disable caching to avoid annoying behaviour when testing
            .handler(StaticHandler.create().setCachingEnabled(false))
        mainRouter.mountSubRouter("/swagger", router)
    }

}