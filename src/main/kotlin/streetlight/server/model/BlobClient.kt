package streetlight.server.model

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url as AwsUrl
import kabinet.console.globalConsole
import kabinet.utils.Environment
import kampfire.model.Url
import kampfire.model.toUrl

class BlobClient(env: Environment) {
    private val bucket = env.read("S3_BUCKET")
    private val endpoint = env.read("S3_ENDPOINT")
    private val region = env.read("S3_REGION")
    private val accessKey = env.read("S3_ACCESS_KEY")
    private val secretKey = env.read("S3_SECRET_KEY")

    private val host = "$bucket.$endpoint"

    private val client = S3Client {
        this@S3Client.region = this@BlobClient.region
        endpointUrl = AwsUrl.parse("https://$endpoint")
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }
    }

    suspend fun put(bytes: ByteArray, filename: String, contentType: String): Url? {
        return try {
            client.putObject {
                this@putObject.bucket = this@BlobClient.bucket
                key = filename
                body = ByteStream.fromBytes(bytes)
                this.contentType = contentType
                acl = ObjectCannedAcl.PublicRead
            }

            "https://$host/$filename".toUrl()
        } catch (e: S3Exception) {
            console.error(e)
            null
        }
    }

    suspend fun delete(url: Url): Boolean {
        if (url.host != host) error("host does not match: ${url.host}")
        val filename = url.filename ?: error("no filename: $url")
        return try {
            client.deleteObject {
                this@deleteObject.bucket = this@BlobClient.bucket
                key = filename
            }
            true
        } catch (e: S3Exception) {
            console.error(e)
            false
        }
    }
}

private val console = globalConsole.getHandle(BlobClient::class)