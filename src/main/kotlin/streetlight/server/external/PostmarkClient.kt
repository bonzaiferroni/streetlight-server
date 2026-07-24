package streetlight.server.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kabinet.utils.Environment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PostmarkClient(env: Environment) {
    private val token = env.read("POSTMARK_SERVER_TOKEN")
    private val fromAddress = env.read("POSTMARK_FROM_ADDRESS")

    private val client = HttpClient(CIO) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }

        defaultRequest {
            url("https://api.postmarkapp.com/")
            header("X-Postmark-Server-Token", token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String,
        tag: String? = null,
        metadata: Map<String, String>? = null,
    ): PostmarkResponse {
        val response = client.post("email") {
            setBody(
                PostmarkEmail(
                    from = fromAddress,
                    to = to,
                    subject = subject,
                    htmlBody = htmlBody,
                    textBody = textBody,
                    tag = tag,
                    metadata = metadata,
                )
            )
        }

        return response.body()
    }
}

@Serializable
data class PostmarkEmail(
    @SerialName("From") val from: String,
    @SerialName("To") val to: String,
    @SerialName("Subject") val subject: String,
    @SerialName("HtmlBody") val htmlBody: String,
    @SerialName("TextBody") val textBody: String,
    @SerialName("MessageStream") val messageStream: String = "outbound",
    @SerialName("TrackOpens") val trackOpens: Boolean = false,
    @SerialName("TrackLinks") val trackLinks: String = "None",
    @SerialName("Tag") val tag: String? = null,
    @SerialName("Metadata") val metadata: Map<String, String>? = null,
)

@Serializable
data class PostmarkResponse(
    @SerialName("To") val to: String,
    @SerialName("SubmittedAt") val submittedAt: String,
    @SerialName("MessageID") val messageId: String,
    @SerialName("ErrorCode") val errorCode: Int,
    @SerialName("Message") val message: String,
)