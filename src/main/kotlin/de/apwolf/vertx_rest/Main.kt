package de.apwolf.vertx_rest

import de.apwolf.vertx_rest.restadapter.configureVertxDefaultJacksonMapper
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import kotlin.system.exitProcess
import org.apache.logging.log4j.kotlin.logger

fun main(args: Array<String>) {
    val logger = logger("de.apwolf.vertx_rest.main()")
    logger.info("Starting up")

    configureVertxDefaultJacksonMapper()

    val options = VertxOptions()
    options.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // To allow hassle-free debugging

    val mainVertx = Vertx.vertx(options)
    mainVertx.deployVerticle(MainVerticle())
        .onFailure { e ->
            logger.error("Error when deploying MainVerticle", e)
            exitProcess(1)
        }
    logger.info("Ready")

}