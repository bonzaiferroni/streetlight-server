@file:OptIn(ExperimentalSerializationApi::class)

package streetlight.server.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import streetlight.model.data.ImageTarget
import streetlight.server.db.tables.sizes
import kampfire.model.Ok
import kampfire.model.toDataOr
import kampfire.model.toOutcome
import klutch.server.apiResponse
import streetlight.model.Api
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import koala.Image
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import streetlight.model.data.ImageFormat
import streetlight.server.service.detectFormatFromImage
import streetlight.server.service.encodeImageAndStore

fun ApiScope.serveUserHub() {
    authGate {
        getApi(Api.Users.Files) {
            error("not implemented")
//            val userId = getUserId()
//            app.dao.userFile.readUserFiles(userId, 100).map { it.url }
        }

        getApi(Api.Users.Talents) { _ ->
            val userId = call.getIdentity().callerId
            dao.talent.readUserTalents(userId).toOutcome()
        }

        postApi(Api.Users.EditTalent) {
            val userId = call.getIdentity().callerId
            val talentId = it.data.talentId
            if (talentId != null) {
                dao.talent.edit(talentId, it.data, userId)
            } else {
                dao.talent.create(it.data, userId)
            }.toOutcome()
        }

        postApi(Api.Users.UploadAvatar) {
            error("not implemented")
//            val bytes = it.data
//            val userId = getUserId()
            // saveBytesAsThumb(bytes, "${userId.value}_avatar", userId)
        }


        post(Api.Users.UploadImage.path) {
            val userId = call.getIdentity().callerId
            var meta: Image? = null
            var target: ImageTarget? = null
            var bytes: ByteArray? = null
            println("ey")

            call.receiveMultipart(formFieldLimit = 16 * 1024 * 1024).forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> when (part.name) {
                        "metadata" -> meta = Json.decodeFromString(part.value)
                        "target" -> target = ImageTarget.valueOf(part.value)
                    }
                    is PartData.FileItem -> bytes = part.provider().readRemaining().readByteArray()
                    else -> {}
                }
                part.release()
            }
            println("received")

            apiResponse {
                val image = encodeImageAndStore(
                    bytes = bytes ?: error("bytes not found"),
                    callerId = userId,
                    sizes = (target ?: error("target not found")).sizes,
                    meta = meta
                ).toDataOr { return@apiResponse it }
                Ok(image)
            }
        }
    }
}

/**
 * The format of an uploaded image, or `null` after responding with the reason it was refused: empty, over 32 MB,
 * unsupported, or not declared as an image.
 */
suspend fun RoutingContext.validateImage(
    bytes: ByteArray,
): ImageFormat? {
    if (bytes.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, "Empty body")
        return null
    }

    // Size guard (example: 32 MB)
    val maxBytes = 32 * 1024 * 1024
    if (bytes.size > maxBytes) {
        call.respond(HttpStatusCode.PayloadTooLarge, "Too large")
        return null
    }

    val fileFormat = detectFormatFromImage(bytes)
    if (fileFormat == null) {
        call.respond(HttpStatusCode.UnsupportedMediaType, "Not a supported image")
        return null
    }

    // Optional: compare with declared Content-Type (don’t trust it, just sanity check)
    val declared = call.request.contentType()
    if (declared.contentType.isNotBlank() && declared.contentType != "image") {
        call.respond(HttpStatusCode.UnsupportedMediaType, "Declared Content-Type not image")
        return null
    }

    return fileFormat
}
