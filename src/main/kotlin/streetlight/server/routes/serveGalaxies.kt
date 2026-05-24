package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.toSlug
import kampfire.model.Ok
import kampfire.model.toResponse
import kampfire.model.responseOf
import klutch.server.ApiContext
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.GalaxyFounded
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostTable
import streetlight.server.model.*
import klutch.server.authGate
import streetlight.model.data.GalaxyContent
import kotlin.time.Clock

private val console = globalConsole.getHandle(ApiContext::serveGalaxies.name)

fun ApiContext.serveGalaxies() {
    val omni = server.get<OmniService>()

    getApi(Api.Galaxies.Top) {
        Ok(dao.galaxy.readTopGalaxies())
    }

    getApi(Api.Galaxies.ReadGalaxySlug, { it.toSlug() }) {
        val id = it.data
        dao.galaxy.readGalaxy(id).toResponse()
    }

    postApi(Api.Galaxies.ReadGalaxies) {
        val galaxyIds = it.data
        Ok(dao.galaxy.readGalaxies(galaxyIds))
    }

    postApi(Api.Galaxies.ReadMultiPosts) {
        val galaxyIds = it.data
        Ok(dao.post.readActivePosts(galaxyIds))
    }

    getApi(Api.Galaxies.ReadPostSlug, { it.toSlug() }) {
        val slug = it.data
        dao.post.readPost(slug).toResponse()
    }

    getApi(Api.Galaxies.ReadPostId, { it.toProjectId() }) {
        val postId = it.data
        dao.post.readPost(postId).toResponse()
    }

    getApi(Api.Galaxies.ReadPosts, { it.toProjectId() }) {
        val galaxyId = it.data
        Ok(dao.post.readActivePosts(galaxyId))
    }

    getApi(Api.Galaxies.ReadContent, { it.toSlug() }) {
        val galaxy = dao.galaxy.readGalaxy(it.data) ?: return@getApi null
        val posts = dao.post.readActivePosts(galaxy.galaxyId)
        Ok(GalaxyContent(galaxy, posts))
    }

    authGate {
        postApi(Api.Galaxies.CreateOrEdit) { request ->
            val edit = request.data
            val identity = call.getIdentity()
            val starId = identity.starId
            val city = edit.cityId?.let { dao.city.readCity(it) ?: error("city not found") }
            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, GalaxyTable.imageConfig)
            when (edit.galaxyId) {
                null -> {
                    dao.galaxy.create(edit, starId, city, imageSet).also { slug ->
                        val name = requireNotNull(edit.name) { "name not found" }
                        omni.sendGalaxyFounded(name, slug, identity.username)
                    }
                }
                else -> {
                    dao.galaxy.update(edit, city, imageSet)
                }
            }.toResponse()
        }

        postApi(Api.Galaxies.CreateEventPost) {
            val request = it.data
            val identity = call.getIdentity()
            responseOf(dao.post.createPost(request, identity))
        }

        postApi(Api.Galaxies.CreatePost) {
            val edit = it.data
            val identity = call.getIdentity()

            val imageSet = saveImages(identity.starId, null, edit.imageRef, PostTable.imageConfig)

            responseOf(dao.post.createPost(edit, identity, imageSet))
        }

        postApi(Api.Galaxies.EditPost) {
            val edit = it.data
            val identity = call.getIdentity()
            val postId = edit.postId ?: error("postId not found")

            val imageSet = saveImages(identity.starId, postId, edit.imageRef, PostTable.imageConfig)

            responseOf(dao.post.editPost(edit, identity, imageSet))
        }

        postApi(Api.Galaxies.CreateLocationPost) {
            val request = it.data
            val identity = call.getIdentity()
            responseOf(dao.post.createPost(request, identity))
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
    }
}
