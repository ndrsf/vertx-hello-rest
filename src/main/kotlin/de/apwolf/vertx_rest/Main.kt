package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.restadapter.configureVertxDefaultJacksonMapper
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val logger = logger("de.apwolf.vertx_rest.main()")
    logger.info("Starting up")

    configureVertxDefaultJacksonMapper()

    val options = VertxOptions()
    options.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // To allow hassle-free debugging


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