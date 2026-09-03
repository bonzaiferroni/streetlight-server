package streetlight.server.routes

import kampfire.api.toSlug
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.toDataOr
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.db.tables.MediaTable
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

fun ApiScope.serveMedia() {

    authGate(optional = true) {
        getApi(Api.Medias.ReadMedia, { it.toSlug() }) {
            val slug = it.data
            HttpProblem.Conflict
            // Ok(dao.media.readMedia(slug) ?: return@getApi HttpProblem.NotFound)
        }
    }

    authGate {
        postApi(Api.Medias.CreateMedia) { request ->
            val edit = request.data
            val identity = call.getIdentity()

            val image = checkImageAndStore(identity.callerId, edit.mediaId, edit.image, MediaTable.imageConfig)
                .toDataOr { return@postApi it }

            dao.media.create(edit.copy(image = image), identity.callerId).toOutcome()
        }

        postApi(Api.Medias.UpdateMedia) { request ->
            val edit = request.data
            val identity = call.getIdentity()
            val image = checkImageAndStore(identity.callerId, edit.mediaId, edit.image, MediaTable.imageConfig)
                .toDataOr { return@postApi it }

            dao.media.update(edit.copy(image = image), identity.callerId).toOutcome()
        }
    }
}