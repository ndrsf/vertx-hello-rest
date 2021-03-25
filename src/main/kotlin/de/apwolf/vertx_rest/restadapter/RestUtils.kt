package de.apwolf.vertx_rest.restadapter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Disables annoying features, allows nice date (de-)serialization, allows Kotlin data class mapping
 */
fun configureJacksonObjectMapper(mapper: ObjectMapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    val module = JavaTimeModule()
    val localDateTimeDeserializer = LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE)
    module.addDeserializer(LocalDate::class.java, localDateTimeDeserializer)
    mapper.registerModule(module)
    mapper.registerModule(KotlinModule())
}

/**
 * Configures Jackson mappers used by Vertx to use our customized mapper configuration
 */
fun configureVertxDefaultJacksonMapper() {
    Json.CODEC as DatabindCodec
    configureJacksonObjectMapper(DatabindCodec.prettyMapper())
    configureJacksonObjectMapper(DatabindCodec.mapper())

}