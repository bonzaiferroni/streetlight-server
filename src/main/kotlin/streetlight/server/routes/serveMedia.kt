package streetlight.server.routes

import kampfire.model.outcomeOf
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

            val imageSet = saveImages(identity.starId, null, edit.imageRef, MediaTable.imageConfig)

            outcomeOf(dao.media.createMedia(edit, identity.starId, imageSet))
        }
    }
}