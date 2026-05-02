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
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.model.data.Post
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveGalaxies.name)

fun StreetlightRouting.serveGalaxies() {

    getEndpoint(Api.Galaxies.Top) {
        dao.galaxy.readTopGalaxies()
    }

    getEndpoint(Api.Galaxies.Path) {
        val pathId = it.data
        dao.galaxy.readGalaxyByPath(pathId)
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
            val request = it.data
            val identity = identity.getIdentity(call)
            Ok(server.dao.post.createPost(request, identity))
        }

        postApi(Api.Galaxies.PostLocation) {
            val request = it.data
            val identity = identity.getIdentity(call)
            val postId = dao.post.createPost(request, identity)

            when (val post = dao.post.readPost(postId)) {
                null -> Problem("Something went wrong.")
                else -> Ok(post)
            }
        }

        getEndpoint(Api.Galaxies.ReadLights) {
            val userId = identity.getUserId(call)
            dao.galaxy.readGalaxyLights(userId)
        }

        postEndpoint(Api.Galaxies.EditLight) {
            val starId = identity.getUserId(call)
            when (val request = it.data) {
                is LightEdit -> {
                    val isSuccess = dao.galaxy.editGalaxyLight(request, starId)
                    if (isSuccess && request.isLit) {
                        service.omni.sendBeacon(starId, request.stringId)
                    }
                    isSuccess
                }

                is MultiLightEdit -> {
                    dao.galaxy.editGalaxyLights(request.edits, starId)
                }
            }
        }

        postApi(Api.Galaxies.RemovePost) {
            val postId = it.data
            val identity = identity.getIdentity(call)
            Ok(dao.post.removePost(postId, identity))
        }
    }
}
