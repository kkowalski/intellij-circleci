package com.circleci.api

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.text.SimpleDateFormat
import java.util.*

object JSON {
    private val jackson: ObjectMapper = jacksonObjectMapper().genericConfig()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    private fun ObjectMapper.genericConfig(): ObjectMapper =
            this.setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"))
                    .setTimeZone(TimeZone.getDefault())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setVisibility(VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE,
                            JsonAutoDetect.Visibility.NONE,
                            JsonAutoDetect.Visibility.NONE,
                            JsonAutoDetect.Visibility.NONE,
                            JsonAutoDetect.Visibility.ANY))

    @JvmStatic
    @Throws(JsonParseException::class)
    fun <T> fromJson(string: String, typeReference: com.fasterxml.jackson.core.type.TypeReference<T>): T {
        return jackson.readValue(string, typeReference)
    }

    @JvmStatic
    @Throws(JsonParseException::class)
    fun toJson(content: Any): String {
        return jackson.writeValueAsString(content)
    }

}