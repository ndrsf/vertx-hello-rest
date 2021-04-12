package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.restadapter.configureVertxDefaultJacksonMapper
import de.apwolf.vertx_rest.util.buildInitialConfig
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.logger
import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
    val logger = logger("de.apwolf.vertx_rest.main()")
    logger.info("Starting up")

    configureVertxDefaultJacksonMapper()

    val initialConfig = buildInitialConfig()
    val options = VertxOptions(initialConfig)

    val mainVertx = Vertx.vertx(options)
    mainVertx.exceptionHandler {
        logger.error("Otherwise uncaught exception in mainVertx", it)
        exitProcess(1)
    }

    GlobalScope.launch(mainVertx.dispatcher()) {
        mainVertx.deployVerticle(MainVerticle()).onFailure {
            logger.error("Exception when deploying MainVerticle", it)
            exitProcess(1)
        }
        logger.info("Ready")
    }

}