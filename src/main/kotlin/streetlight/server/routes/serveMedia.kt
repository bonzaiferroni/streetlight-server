package streetlight.server.routes

import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.db.tables.MediaTable
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

fun ApiScope.serveMedia() {
    authGate {
        postApi(Api.Medias.CreateMedia) {
            val edit = it.data
            val identity = call.getIdentity()

            val image = checkImageAndStore(identity.callerId, edit.mediaId, edit.image, MediaTable.imageConfig)

            dao.media.create(edit.copy(image = image), identity.callerId).toOutcome()
        }

        postApi(Api.Medias.UpdateMedia) {
            val edit = it.data
            val identity = call.getIdentity()
            val image = checkImageAndStore(identity.callerId, edit.mediaId, edit.image, MediaTable.imageConfig)

            dao.media.update(edit.copy(image = image), identity.callerId).toOutcome()
        }
    }
}