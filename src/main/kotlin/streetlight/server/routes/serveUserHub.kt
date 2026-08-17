@file:OptIn(ExperimentalSerializationApi::class)

package streetlight.server.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kampfire.model.ImageSize
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
            var bytes: ByteArray? = null
            println("ey")

            call.receiveMultipart(formFieldLimit = 16 * 1024 * 1024).forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> if (part.name == "metadata") {
                        meta = Json.decodeFromString(part.value)
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
                    sizes = ImageSize.All,
                    meta = meta
                ).toDataOr { return@apiResponse it }
                Ok(image)
            }
        }
    }
}

