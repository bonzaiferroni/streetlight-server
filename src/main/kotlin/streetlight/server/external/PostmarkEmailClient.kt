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
import kampfire.api.EmailAddress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import streetlight.server.model.Email
import streetlight.server.model.EmailClient
import streetlight.server.model.SendEmailResult

class PostmarkEmailClient(env: Environment): EmailClient {
    private val token = env.read("POSTMARK_SERVER_TOKEN")
    // private val fromAddress = env.read("POSTMARK_FROM_ADDRESS")

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

    override suspend fun sendEmail(email: Email): SendEmailResult {
        val response: PostmarkResponse = client.post("email") {
            setBody(email.toPostmarkEmail())
        }.body()

        return response.toMailResult()
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

fun Email.toPostmarkEmail() = PostmarkEmail(
    from = from.value,
    to = to.value,
    subject = subject,
    htmlBody = htmlBody,
    textBody = textBody,
    tag = tag,
    metadata = metadata
)

@Serializable
data class PostmarkResponse(
    @SerialName("To") val to: String,
    @SerialName("SubmittedAt") val submittedAt: String,
    @SerialName("MessageID") val messageId: String,
    @SerialName("ErrorCode") val errorCode: Int,
    @SerialName("Message") val message: String,
)

fun PostmarkResponse.toMailResult() = SendEmailResult(
    to = EmailAddress(to),
    submittedAt = submittedAt,
    messageId = messageId,
    errorCode = errorCode,
    message = message
)

@Serializable
data class PostmarkBounce(
    @SerialName("RecordType") val recordType: String,
    @SerialName("Type") val type: String,
    @SerialName("TypeCode") val typeCode: Int,
    @SerialName("Email") val email: String,
    @SerialName("BouncedAt") val bouncedAt: String,
    @SerialName("Description") val description: String = "",
    @SerialName("Details") val details: String = "",
    @SerialName("MessageID") val messageId: String = "",
    @SerialName("Inactive") val inactive: Boolean = false,
    @SerialName("CanActivate") val canActivate: Boolean = true,
)