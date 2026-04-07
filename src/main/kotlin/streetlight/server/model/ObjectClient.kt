package streetlight.server.model

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import kabinet.console.globalConsole

class ObjectClient(
    private val bucket: String,
    private val endpoint: String,
    private val region: String,
    private val accessKey: String,
    private val secretKey: String
) {
    private val client = S3Client {
        this@S3Client.region = this@ObjectClient.region
        endpointUrl = Url.parse("https://$endpoint")
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }
    }

    suspend fun upload(bytes: ByteArray, filename: String, contentType: String): String? {
        return try {
            client.putObject {
                this@putObject.bucket = this@ObjectClient.bucket
                key = filename
                body = ByteStream.fromBytes(bytes)
                this.contentType = contentType
                acl = ObjectCannedAcl.PublicRead
            }

            "https://$bucket.$endpoint/$filename"
        } catch (e: S3Exception) {
            console.logThrowable(e)
            null
        }
    }
}

private val console = globalConsole.getHandle(ObjectClient::class)