package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.Ok
import kampfire.model.Problem
import klutch.server.authenticateJwt
import klutch.server.getApi
import klutch.server.getEndpoint
import klutch.server.postApi
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.model.data.GalaxyFounded
import streetlight.model.data.GalaxyId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveGalaxies.name)

fun StreetlightRouting.serveGalaxies() {

    getEndpoint(Api.Galaxies.Top) {
        dao.galaxy.readTopGalaxies()
    }

    getApi(Api.Galaxies.ReadSlug, { it }) {
        val pathId = it.data
        responseOf(dao.galaxy.readGalaxySlug(pathId))
    }

    getApi(Api.Galaxies.ReadId, { GalaxyId(it) }) {
        val galaxyId = it.data
        responseOf(dao.galaxy.readGalaxy(galaxyId))
    }

    postEndpoint(Api.Galaxies.ReadGalaxies) {
        val galaxyIds = it.data
        dao.galaxy.readGalaxies(galaxyIds)
    }

    postApi(Api.Galaxies.ReadMultiPosts) {
        val galaxyIds = it.data
        Ok(server.dao.post.readActivePosts(galaxyIds))
    }

    getApi(Api.Galaxies.ReadPost, { it.toProjectId() }) { request ->
        val postId = request.data
        val post = server.dao.post.readPost(postId)
        post?.let { Ok(it) } ?: Problem("Post not found: $postId")
    }

    getApi(Api.Galaxies.ReadPosts, { it.toProjectId() }) {
        val galaxyId = it.data
        Ok(server.dao.post.readActivePosts(galaxyId))
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            val edit = request.data
            val identity = identity.getIdentity(call)
            val starId = identity.userId
            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, GalaxyTable.imageConfig)
            val galaxy = dao.galaxy.create(edit, starId, imageSet)
            if (galaxy != null) {
                server.service.omni.sendMessage(
                    GalaxyFounded(
                        galaxyId = galaxy.galaxyId,
                        name = galaxy.name,
                        username = identity.username,
                        recordAt = galaxy.createdAt
                    )
                )
            }
            galaxy
        }

        postApi(Api.Galaxies.PostEvent) {
            val request = it.data
            val identity = identity.getIdentity(call)
            when (val postId = dao.post.createPost(request, identity)) {
                null -> Problem("Something went wrong.")
                else -> {
                    when (val post = dao.post.readPost(postId)) {
                        null -> Problem("Something went wrong.")
                        else -> Ok(post)
                    }
                }
            }
        }

        postApi(Api.Galaxies.PostContent) {
            val edit = it.data
            val identity = identity.getIdentity(call)

            val imageSet = saveImages(identity.userId, null, edit.imageRef, PostTable.imageConfig)

            val postId = dao.post.createPost(edit, identity, imageSet)

            responseOf(dao.post.readPost(postId))
        }

        postApi(Api.Galaxies.EditContent) {
            val edit = it.data
            val identity = identity.getIdentity(call)
            val postId = edit.postId ?: error("postId not found")

            val imageSet = saveImages(identity.userId, postId, edit.imageRef, PostTable.imageConfig)

            val isSuccess = dao.post.editPost(edit, identity, imageSet)
            if (!isSuccess) {
                return@postApi null
            }

            responseOf(dao.post.readPost(postId))
        }

        postApi(Api.Galaxies.PostLocation) {
            val request = it.data
            val identity = identity.getIdentity(call)
            val postId = dao.post.createPost(request, identity)

            responseOf(dao.post.readPost(postId))
        }

        getEndpoint(Api.Galaxies.ReadLights) {
            val userId = identity.getUserId(call)
            dao.light.readGalaxyLights(userId)
        }

        postApi(Api.Galaxies.RemovePost) {
            val postId = it.data
            val identity = identity.getIdentity(call)
            Ok(dao.post.removePost(postId, identity))
        }
    }
}
