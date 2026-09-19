package streetlight.server

import kabinet.utils.Environment

fun readTestEnvironment(
    buildMode: BuildMode = BuildMode.Development,
    dbUrl: String = "jdbc:postgresql://localhost:5432/streetlight",
    dbUser: String = "test",
    dbPassword: String = "test",
    postmarkWebhookSecret: String = "test-webhook-secret",
    geminiKey: String = "test-gemini-key",
): Environment = Environment.fromText(
    """
    ${EnvKey.BUILD_ENV}=${buildMode.name.lowercase()}
    ${EnvKey.DB_URL}=$dbUrl
    ${EnvKey.DB_USER}=$dbUser
    ${EnvKey.DB_PASSWORD}=$dbPassword
    $POSTMARK_WEBHOOK_SECRET=$postmarkWebhookSecret
    $GEMINI_KEY_A=$geminiKey
    """.trimIndent()
)

private const val POSTMARK_WEBHOOK_SECRET = "POSTMARK_WEBHOOK_SECRET"
private const val GEMINI_KEY_A = "GEMINI_KEY_A"
