package streetlight.server.routes

import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import streetlight.model.Api
import streetlight.model.data.GalaxyFounded
import streetlight.model.data.LightEdit
import streetlight.model.data.MultiLightEdit
import streetlight.model.data.PostListing
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveGalaxies.name)

fun StreetlightRouting.serveGalaxies() {
    val dao = server.dao.galaxy

    getEndpoint(Api.Galaxies.Top) {
        dao.readTopGalaxies()
    }

    getEndpoint(Api.Galaxies.Path) {
        val pathId = it.data
        dao.readGalaxyByPath(pathId)
    }
    
    postEndpoint(Api.Galaxies.ReadGalaxies) {
        val galaxyIds = it.data
        dao.readGalaxies(galaxyIds)
    }

    postEndpoint(Api.Galaxies.ReadMultiPosts) {
        val galaxyIds = it.data
        server.dao.eventPost.readPosts(galaxyIds)
    }

    getEndpoint(Api.Galaxies.ReadPost, { it.toProjectId() }) { galaxyPostId, _ ->
        server.dao.eventPost.readPost(galaxyPostId)
    }

    getEndpoint(Api.Galaxies.ReadPosts, { it.toProjectId()}) { galaxyId, _ ->
        val events = server.dao.eventPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        val locations = server.dao.locationPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        PostListing(
            events = events,
            locations = locations
        )
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            val edit = request.data
            val identity = identity.getIdentity(call)
            val starId = identity.userId
            val imageUserId = starId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, GalaxyTable.imageConfig)
            val galaxy = dao.create(edit, starId, imageSet)
            if (galaxy != null) {
                server.service.omni.sendMessage(GalaxyFounded(
                    galaxyId = galaxy.galaxyId,
                    name = galaxy.name,
                    username = identity.username,
                    recordAt = galaxy.createdAt
                ))
            }
            galaxy
        }

        postEndpoint(Api.Galaxies.PostEvent) {
            val request = it.data
            val identity = identity.getIdentity(call)
            server.dao.eventPost.create(request, identity)
        }

        postEndpoint(Api.Galaxies.PostLocation) {
            val request = it.data
            val identity = identity.getIdentity(call)
            server.dao.locationPost.createPost(request, identity)
        }

        getEndpoint(Api.Galaxies.ReadLights) {
            val userId = identity.getUserId(call)
            dao.readGalaxyLights(userId)
        }

        postEndpoint(Api.Galaxies.EditLight) {
            val starId = identity.getUserId(call)
            when (val request = it.data) {
                is LightEdit -> {
                    val isSuccess = dao.editGalaxyLight(request, starId)
                    if (isSuccess && request.isLit) {
                        server.service.omni.sendBeacon(starId, request.stringId)
                    }
                    isSuccess
                }
                is MultiLightEdit -> {
                    dao.editGalaxyLights(request.edits, starId)
                }
            }
        }
    }
}
