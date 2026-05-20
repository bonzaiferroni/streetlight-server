package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.Ok
import kampfire.model.Problem
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

private val console = globalConsole.getHandle(ApiContext::serveGalaxies.name)

fun ApiContext.serveGalaxies() {
    val omni = server.get<OmniService>()

    getApi(Api.Galaxies.Top) {
        Ok(dao.galaxy.readTopGalaxies())
    }

    getApi(Api.Galaxies.ReadSlug, { it }) {
        val pathId = it.data
        responseOf(dao.galaxy.readGalaxySlug(pathId))
    }

    getApi(Api.Galaxies.ReadId, { it }) {
        val id = it.data
        responseOf(dao.galaxy.readGalaxy(id))
    }

    postApi(Api.Galaxies.ReadGalaxies) {
        val galaxyIds = it.data
        Ok(dao.galaxy.readGalaxies(galaxyIds))
    }

    postApi(Api.Galaxies.ReadMultiPosts) {
        val galaxyIds = it.data
        Ok(dao.post.readActivePosts(galaxyIds))
    }

    getApi(Api.Galaxies.ReadPost, { it }) { request ->
        val id = request.data
        val post = dao.post.readPost(id)
        post?.let { Ok(it) } ?: Problem("Post not found: $id")
    }

    getApi(Api.Galaxies.ReadPosts, { it.toProjectId() }) {
        val galaxyId = it.data
        Ok(dao.post.readActivePosts(galaxyId))
    }

    authGate {
        postApi(Api.Galaxies.CreateOrEdit) { request ->
            val edit = request.data
            val identity = call.getIdentity()
            val starId = identity.starId
            val city = edit.cityId?.let { dao.locality.readCity(it) ?: error("city not found") }
            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, GalaxyTable.imageConfig)
            when (edit.galaxyId) {
                null -> {
                    val galaxy = dao.galaxy.create(edit, starId, city, imageSet)
                    if (galaxy != null) {
                        omni.sendMessage(
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
                else -> {
                    dao.galaxy.update(edit, city, imageSet)
                }
            }.toResponse()
        }

        postApi(Api.Galaxies.PostEvent) {
            val request = it.data
            val identity = call.getIdentity()
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
            val identity = call.getIdentity()

            val imageSet = saveImages(identity.starId, null, edit.imageRef, PostTable.imageConfig)

            val postId = dao.post.createPost(edit, identity, imageSet)

            responseOf(dao.post.readPost(postId))
        }

        postApi(Api.Galaxies.EditContent) {
            val edit = it.data
            val identity = call.getIdentity()
            val postId = edit.postId ?: error("postId not found")

            val imageSet = saveImages(identity.starId, postId, edit.imageRef, PostTable.imageConfig)

            val isSuccess = dao.post.editPost(edit, identity, imageSet)
            if (!isSuccess) {
                return@postApi null
            }

            responseOf(dao.post.readPost(postId))
        }

        postApi(Api.Galaxies.PostLocation) {
            val request = it.data
            val identity = call.getIdentity()
            val postId = dao.post.createPost(request, identity)

            responseOf(dao.post.readPost(postId))
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
