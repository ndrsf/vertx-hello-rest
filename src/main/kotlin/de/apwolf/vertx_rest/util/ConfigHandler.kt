package de.apwolf.vertx_rest.util

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.apache.logging.log4j.kotlin.Logging

/**
 * Want the current config? Send an event like this:
 *
 * val reply = awaitResult<Message<JsonObject>> { vertx.eventBus().request(GET_CURRENT_CONFIG, "", it) }
 */
class ConfigHandler : Logging, CoroutineVerticle() {

    var currentConfig: JsonObject? = null

    override suspend fun start() {
        val retriever = ConfigRetriever.create(vertx, buildConfigRetrieverOptions())
        retriever.listen {
            logger.info("Retrieved new config")
            currentConfig = it.newConfiguration
        }

        currentConfig = retriever.config.await()

        vertx.eventBus().consumer<Unit>(GET_CURRENT_CONFIG).handler { it.reply(currentConfig) }

        super.start()
    }
}

fun buildConfigRetrieverOptions(): ConfigRetrieverOptions {
    val configOptions = ConfigStoreOptions()
    configOptions.format = "yaml"
    configOptions.type = "file"
    configOptions.config = JsonObject().put("path", "vertx-config.yaml")
    return ConfigRetrieverOptions().addStore(configOptions)
}

/**
 * Explicit bootstrapping config in case we want to configure the startup
 */
suspend fun buildInitialConfig(): JsonObject {
    val bootstrapVertx = Vertx.vertx()

    val config = ConfigRetriever
        .create(bootstrapVertx, buildConfigRetrieverOptions())
        .config
        .await()

    bootstrapVertx.close()
    return config
}