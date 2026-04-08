package streetlight.server.routes

import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.server.getEndpoint
import klutch.server.postEndpoint
import klutch.utils.getUserId
import klutch.utils.getUserIdentity
import streetlight.model.Api
import streetlight.model.data.PostListing
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.EventTable
import streetlight.server.model.*

private val console = globalConsole.getHandle(StreetlightRouting::serveGalaxies.name)

fun StreetlightRouting.serveGalaxies() {
    val dao = app.dao.galaxy

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
        app.dao.eventPost.readPosts(galaxyIds)
    }

    getEndpoint(Api.Galaxies.ReadPost, { it.toProjectId() }) { galaxyPostId, _ ->
        app.dao.eventPost.readPost(galaxyPostId)
    }

    getEndpoint(Api.Galaxies.ReadPosts, { it.toProjectId()}) { galaxyId, _ ->
        val events = app.dao.eventPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        val locations = app.dao.locationPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        PostListing(
            events = events,
            locations = locations
        )
    }

    authenticateJwt {
        postEndpoint(Api.Galaxies.Found) { request ->
            val edit = request.data
            val userId = getUserId()
            val imageUserId = userId.takeIf { edit.imageRef?.isRelative ?: false }
            val imageSet = saveImages(imageUserId, edit.galaxyId, edit.imageRef, EventTable.imageConfig)
            dao.create(edit, userId, imageSet)
        }

        postEndpoint(Api.Galaxies.PostEvent) { request ->
            val identity = getUserIdentity()

            app.dao.eventPost.create(request.data, identity)
        }

        getEndpoint(Api.Galaxies.ReadLights) {
            val userId = getUserId()
            dao.readGalaxyLights(userId)
        }

        postEndpoint(Api.Galaxies.EditLight) {
            val userId = getUserId()
            dao.editGalaxyLight(it.data, userId)
        }
    }
}
