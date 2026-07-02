package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.api.toSlug
import kampfire.model.Ok
import kampfire.model.toOutcome
import kampfire.model.outcomeOf
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostTable
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.provide
import streetlight.model.data.City
import streetlight.model.data.GalaxyContent
import streetlight.model.data.GalaxyEdit
import streetlight.server.db.tables.SavedImageSet

private val console = globalConsole.getHandle(ApiScope::serveGalaxies.name)

fun ApiScope.serveGalaxies() {
    val omni = provide<OmniService>()

    authGate(optional = true) {
        getApi(Api.Galaxies.Top) {
            val identity = call.getIdentityOrNull()
            Ok(dao.galaxy.readTopGalaxies(identity?.starId))
        }

        getApi(Api.Galaxies.ReadGalaxySlug, { it.toSlug() }) {
            val id = it.data
            val identity = call.getIdentityOrNull()
            dao.galaxy.readGalaxy(id, identity?.starId).toOutcome()
        }

        postApi(Api.Galaxies.ReadGalaxies) {
            val galaxyIds = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.galaxy.readGalaxies(galaxyIds, identity?.starId))
        }

        postApi(Api.Galaxies.ReadMultiPosts) {
            val galaxyIds = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.post.readOrderedPosts(galaxyIds, identity?.starId))
        }

        getApi(Api.Galaxies.ReadPostId, { it.toRecordId() }) {
            val postId = it.data
            val identity = call.getIdentityOrNull()
            dao.post.readPost(postId, identity?.starId).toOutcome()
        }

        getApi(Api.Galaxies.ReadPosts, { it.toRecordId() }) {
            val galaxyId = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.post.readOrderedPosts(galaxyId, identity?.starId))
        }

        getApi(Api.Galaxies.ReadContent, { it.toSlug() }) {
            val identity = call.getIdentityOrNull()
            val galaxy = dao.galaxy.readGalaxy(it.data, identity?.starId) ?: return@getApi null
            val posts = dao.post.readOrderedPosts(galaxy.galaxyId, identity?.starId)
            Ok(GalaxyContent(galaxy, posts))
        }
    }

    authGate {
        suspend fun handleEdit(
            edit: GalaxyEdit,
            identity: StarIdentity,
            block: suspend (City, SavedImageSet) -> Slug?
        ): Slug? {
            val starId = identity.starId
            val city = edit.cityId?.let { dao.city.readCity(it) }
            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, GalaxyTable.imageConfig)

            return block(requireNotNull(city), requireNotNull(imageSet))
        }

        postApi(Api.Galaxies.CreateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, imageSet ->
                dao.galaxy.create(edit, identity.starId, city, imageSet).also { slug ->
                    val name = requireNotNull(edit.name) { "name not found" }
                    omni.sendGalaxyFounded(name, slug, identity.username)
                }
            }.toOutcome()
        }

        postApi(Api.Galaxies.UpdateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, imageSet ->
                dao.galaxy.update(edit, city, imageSet)
            }.toOutcome()
        }

        postApi(Api.Galaxies.EditPost) {
            val edit = it.data
            val identity = call.getIdentity()
            val postId = edit.postId ?: error("postId not found")

            outcomeOf(dao.post.editPost(postId, edit, identity))
        }

        getApi(Api.Galaxies.ReadLights) {
            val userId = call.getIdentity().starId
            Ok(dao.light.readGalaxyLights(userId))
        }

        postApi(Api.Galaxies.RemovePost) {
            val postId = it.data
            val identity = call.getIdentity()
            Ok(dao.post.removePost(postId, identity))
        }

        getApi(Api.Galaxies.ReadUserGalaxies) {
            val starId = call.getIdentity().starId
            dao.galaxy.readGalaxies(starId).toOutcome()
        }
    }
}
