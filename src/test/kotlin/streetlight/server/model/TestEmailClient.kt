package streetlight.server.model

import kotlin.time.Clock

class TestEmailClient(
    private val emailRouter: EmailRouter,
    private val getErrorCode: () -> Int = { 0 },
): EmailClient {

    override suspend fun sendEmail(email: Email): SendEmailResult {
        val errorCode = getErrorCode()

        val message = when (errorCode) {
            0 -> {
                emailRouter.deliverEmail(email)
                "OK"
            }
            else -> "Failed to send email"
        }

        return SendEmailResult(
            to = email.to,
            submittedAt = Clock.System.now().toString(),
            messageId = "test-message-id",
            errorCode = errorCode,
            message = message
        )
    }
}