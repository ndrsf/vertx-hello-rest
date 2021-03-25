package de.apwolf.vertx_rest.restadapter

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import org.apache.logging.log4j.kotlin.Logging

/**
 * Swagger UI is available on localhost:8085/swagger/swagger-ui
 *
 * I would really like to make the path to the UI easier (just /swagger) but I can't seem to get it working.
 */
class SwaggerUiVerticle : AbstractVerticle(), Logging {

    // Visible to access it from main to configure routing, lateinit because has to be init in start()
    lateinit var router: Router

    override fun start(startPromise: Promise<Void>) {
        router = Router.router(vertx)
        router
            .route("/*")
            .handler(
                StaticHandler
                    .create()
                    .setCachingEnabled(false)
            )
        super.start(startPromise)
    }

}