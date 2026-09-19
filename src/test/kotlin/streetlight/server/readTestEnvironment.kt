package streetlight.server

import kabinet.utils.Environment

fun readTestEnvironment(
    buildMode: BuildMode = BuildMode.Development,
    dbUrl: String = "jdbc:postgresql://localhost:5432/streetlight",
    dbUser: String = "test",
    dbPassword: String = "test",
    supportAddress: String = "support@streetlight.test",
): Environment = Environment.fromText(
    """
    ${EnvKey.BUILD_ENV}=${buildMode.name.lowercase()}
    ${EnvKey.DB_URL}=$dbUrl
    ${EnvKey.DB_USER}=$dbUser
    ${EnvKey.DB_PASSWORD}=$dbPassword
    STREETLIGHT_SUPPORT_ADDRESS=$supportAddress
    POSTMARK_WEBHOOK_SECRET=test-webhook-secret
    POSTMARK_SERVER_TOKEN=test-postmark-token
    GEMINI_KEY_A=test-gemini-key
    REPLICATE_KEY=test-replicate-key
    SERVER_IP=127.0.0.1
    SERVER_PORT=0
    S3_BUCKET=test-bucket
    S3_ENDPOINT=https://s3.test.invalid
    S3_REGION=test-region
    S3_ACCESS_KEY=test-access-key
    S3_SECRET_KEY=test-secret-key
    """.trimIndent()
)
