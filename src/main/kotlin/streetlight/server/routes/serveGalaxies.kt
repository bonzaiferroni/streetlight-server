package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.api.toSlug
import kampfire.model.Ok
import kampfire.model.toOutcome
import kampfire.model.outcomeOf
import klutch.db.model.Identity
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.model.*
import klutch.server.authGate
import klutch.server.provide
import streetlight.model.data.City
import streetlight.model.data.GalaxyContent
import streetlight.model.data.GalaxyEdit

private val console = globalConsole.getHandle(ApiScope::serveGalaxies.name)

fun ApiScope.serveGalaxies() {
    val omni = provide<OmniService>()

    authGate(optional = true) {
        getApi(Api.Galaxies.Top) {
            val identity = call.getIdentityOrNull()
            Ok(dao.galaxy.readTopGalaxies(identity?.callerId))
        }

        getApi(Api.Galaxies.ReadGalaxySlug, { it.toSlug() }) {
            val id = it.data
            val identity = call.getIdentityOrNull()
            dao.galaxy.readGalaxy(id, identity?.callerId).toOutcome()
        }

        postApi(Api.Galaxies.ReadGalaxies) {
            val galaxyIds = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.galaxy.readGalaxies(galaxyIds, identity?.callerId))
        }

        postApi(Api.Galaxies.ReadMultiPosts) {
            val galaxyIds = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.post.readOrderedPosts(galaxyIds, identity?.callerId))
        }

        getApi(Api.Galaxies.ReadPostId, { it.toRecordId() }) {
            val postId = it.data
            val identity = call.getIdentityOrNull()
            dao.post.readPost(postId, identity?.callerId).toOutcome()
        }

        getApi(Api.Galaxies.ReadPosts, { it.toRecordId() }) {
            val galaxyId = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.post.readOrderedPosts(galaxyId, identity?.callerId))
        }

        getApi(Api.Galaxies.ReadContent, { it.toSlug() }) {
            val identity = call.getIdentityOrNull()
            val galaxy = dao.galaxy.readGalaxy(it.data, identity?.callerId) ?: return@getApi null
            val posts = dao.post.readOrderedPosts(galaxy.galaxyId, identity?.callerId)
            Ok(GalaxyContent(galaxy, posts))
        }
    }

    authGate {
        suspend fun handleEdit(
            edit: GalaxyEdit,
            identity: Identity,
            block: suspend (City?, GalaxyEdit) -> Slug?
        ): Slug? {
            val starId = identity.callerId
            val city = edit.cityId?.let { dao.city.readCity(it) }
            val imageUserId = starId.takeIf { edit.image?.isRelative ?: false }
            val image = checkImageAndStore(imageUserId, edit.galaxyId, edit.image, GalaxyTable.imageConfig)

            return block(city, edit.copy(image = image))
        }

        postApi(Api.Galaxies.CreateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, edit ->
                dao.galaxy.create(edit, identity.callerId, city).also { slug ->
                    val name = requireNotNull(edit.name) { "name not found" }
                    omni.sendGalaxyFounded(name, slug, identity.username)
                }
            }.toOutcome()
        }

        postApi(Api.Galaxies.UpdateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, edit ->
                dao.galaxy.update(edit, city)
            }.toOutcome()
        }

        postApi(Api.Galaxies.UpdatePost) {
            val edit = it.data
            val identity = call.getIdentity()
            val postId = edit.postId ?: error("postId not found")

            dao.post.update(postId, edit, identity.callerId).toOutcome()
        }

        getApi(Api.Galaxies.ReadLights) {
            val callerId = call.getIdentity().callerId
            Ok(dao.light.readGalaxyLights(callerId))
        }

        postApi(Api.Galaxies.RemovePost) {
            val postId = it.data
            val identity = call.getIdentity()
            Ok(dao.post.removePost(postId, identity))
        }

        getApi(Api.Galaxies.ReadUserGalaxies) {
            val callerId = call.getIdentity().callerId
            dao.galaxy.readGalaxies(callerId).toOutcome()
        }

        postApi(Api.Galaxies.CreatePost) {
            val callerId = call.getIdentity().callerId
            dao.post.create(it.data, callerId).toOutcome()
        }
    }
}
