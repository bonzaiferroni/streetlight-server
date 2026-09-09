package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.Slug
import kampfire.api.toSlug
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toOutcome
import kampfire.model.toDataOr
import kampfire.model.toOk
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
import streetlight.model.data.GalaxyConfig
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.Mark

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
            // td: add marks
            dao.post.readPost(postId).toOutcome()
        }

        getApi(Api.Galaxies.ReadPosts, { it.toRecordId() }) {
            val galaxyId = it.data
            val identity = call.getIdentityOrNull()
            Ok(dao.post.readOrderedPosts(galaxyId, identity?.callerId))
        }

        getApi(Api.Galaxies.ReadContent, { it.toSlug() }) {
            val identity = call.getIdentityOrNull()
            Ok(readGalaxyContent(it.data, identity?.callerId) ?: return@getApi HttpProblem.NotFound)
        }
    }

    authGate {
        suspend fun handleEdit(
            edit: GalaxyEdit,
            identity: Identity,
            block: suspend (City?, GalaxyEdit) -> Outcome<Slug>
        ): Outcome<Slug> {
            val starId = identity.callerId
            val city = edit.cityId?.let { dao.city.readCity(it) }
            val imageUserId = starId.takeIf { edit.image?.isRelative ?: false }
            val image = checkImageAndStore(imageUserId, edit.galaxyId, edit.image, GalaxyTable.imageConfig)
                .toDataOr { return it }

            return block(city, edit.copy(image = image))
        }

        postApi(Api.Galaxies.CreateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, edit ->
                dao.galaxy.create(edit, identity.callerId, city).also { slug ->
                    val name = requireNotNull(edit.name) { "name not found" }
                    omni.sendGalaxyFounded(name, slug, identity.username)
                }.toOk()
            }
        }

        postApi(Api.Galaxies.UpdateGalaxy) {
            val edit = it.data
            val identity = call.getIdentity()
            handleEdit(edit, identity) { city, edit ->
                dao.galaxy.update(edit, city).toOk()
            }
        }

        postApi(Api.Galaxies.UpdatePost) {
            val edit = it.data
            val identity = call.getIdentity()
            val postId = edit.postId ?: error("postId not found")

            dao.post.update(postId, edit, identity.callerId).toOutcome()
        }

        getApi(Api.Galaxies.ReadConfig, { it.toSlug() }) {
            val slug = it.data
            val identity = call.getIdentity()
            // gate here by identity or admin?
            val galaxy = dao.galaxy.readGalaxy(slug, identity.callerId) ?: return@getApi HttpProblem.NotFound
            val marks = dao.galaxy.readFeedMarks(galaxy.galaxyId)
            Ok(GalaxyConfig(galaxy, marks))
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

        postApi(Api.Galaxies.ProvisionMark) { request ->
            val name = request.data.trim().takeIf { it.length in Mark.ValidLength }
                ?: return@postApi HttpProblem.BadRequest
            Ok(dao.galaxy.provisionMark(name))
        }

        postApi(Api.Galaxies.UpdateMark) {
            dao.post.updateMark(it.data, call.getIdentity().callerId)
            Ok(Unit)
        }
    }
}
