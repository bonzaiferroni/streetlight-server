package streetlight.server.routes

import kampfire.model.outcomeOf
import klutch.server.authGate
import klutch.server.postApi
import streetlight.model.Api
import streetlight.server.db.tables.MediumTable
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

fun ApiScope.serveMedium() {
    authGate {
        postApi(Api.Media.CreateMedia) {
            val edit = it.data
            val identity = call.getIdentity()

            val imageSet = saveImages(identity.starId, null, edit.imageRef, MediumTable.imageConfig)

            outcomeOf(dao.medium.createMedium(edit, identity.starId, imageSet))
        }
    }
}