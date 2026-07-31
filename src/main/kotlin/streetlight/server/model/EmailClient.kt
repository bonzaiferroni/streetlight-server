package streetlight.server.model

import kampfire.api.EmailAddress

interface EmailClient {
    suspend fun sendEmail(email: Email): SendEmailResult
}

data class Email(
    val from: EmailAddress,
    val to: EmailAddress,
    val subject: String,
    val htmlBody: String,
    val textBody: String,
    val tag: String? = null,
    val metadata: Map<String, String>? = null,
)

data class SendEmailResult(
    val to: EmailAddress,
    val submittedAt: String,
    val messageId: String,
    val errorCode: Int,
    val message: String,
)