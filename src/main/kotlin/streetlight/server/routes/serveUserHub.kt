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
import streetlight.server.service.editTalent
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
            editTalent(call.getIdentity().callerId, it.data)
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
