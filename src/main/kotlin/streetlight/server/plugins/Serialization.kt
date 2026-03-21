package streetlight.server.plugins

import io.ktor.serialization.kotlinx.cbor.cbor
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            json = kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
                prettyPrint = true

            }
        )
        cbor(Cbor {
            ignoreUnknownKeys = true
        })
    }
}
