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
import kampfire.model.Url
import kampfire.model.toUrl

class ObjectStorageClient(
    private val bucket: String,
    private val endpoint: String,
    private val region: String,
    private val accessKey: String,
    private val secretKey: String
) {
    private val host = "$bucket.$endpoint"

    private val client = S3Client {
        this@S3Client.region = this@ObjectStorageClient.region
        endpointUrl = AwsUrl.parse("https://$endpoint")
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }
    }

    suspend fun put(bytes: ByteArray, filename: String, contentType: String): Url? {
        return try {
            client.putObject {
                this@putObject.bucket = this@ObjectStorageClient.bucket
                key = filename
                body = ByteStream.fromBytes(bytes)
                this.contentType = contentType
                acl = ObjectCannedAcl.PublicRead
            }

            "https://$host/$filename".toUrl()
        } catch (e: S3Exception) {
            console.logThrowable(e)
            null
        }
    }

    suspend fun delete(url: Url): Boolean {
        if (url.host != host) error("host does not match: ${url.host}")
        val filename = url.filename ?: error("no filename: $url")
        return try {
            client.deleteObject {
                this@deleteObject.bucket = this@ObjectStorageClient.bucket
                key = filename
            }
            true
        } catch (e: S3Exception) {
            console.logThrowable(e)
            false
        }
    }
}

private val console = globalConsole.getHandle(ObjectStorageClient::class)