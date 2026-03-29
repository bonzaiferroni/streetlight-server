package streetlight.server.routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import klutch.server.authenticateJwt
import klutch.utils.getUserIdOrNull
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import streetlight.server.SiteStyles
import streetlight.web.pages.eventPage
import streetlight.web.pages.eventPortal
import streetlight.web.pages.eventSignUp
import streetlight.web.pages.galaxyProfilePage
import streetlight.web.pages.homePage
import streetlight.web.pages.locationPage
import streetlight.web.shells.GalaxyProfileContent
import streetlight.web.shells.HomeContent
import java.io.File

private val console = globalConsole.getHandle(Routing::servePages.name)

fun Routing.servePages(app: ServerProvider = RuntimeProvider) {


    get("/event-portal/{id}") {
        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
        val event = app.dao.event.readEvent(eventId) ?: return@get
        val spark = app.dao.spark.readByUserId(event.userId)
        val requestItems = app.dao.song.readRequestItems(event.userId)
        call.respondHtml {
            console.log("responding")
            eventPortal(event, spark, requestItems, SiteStyles)
        }
    }

    get("/event-signup/{id}") {
        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
        val event = app.dao.event.readEvent(eventId) ?: return@get
        call.respondHtml {
            eventSignUp(event, SiteStyles)
        }
    }

    get("/event/{id}") {
        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
        val event = app.dao.event.readEvent(eventId) ?: return@get

        call.respondHtml {
            eventPage(event, SiteStyles)
        }
    }

    get("/location/{id}") {
        val locationId = call.parameters["id"]?.let { LocationId(it) } ?: return@get
        val location = app.dao.location.readLocation(locationId) ?: return@get

        call.respondHtml {
            locationPage(location, SiteStyles)
        }
    }

    authenticateJwt(optional = true) {
        get("/") {
            val userId = getUserIdOrNull()
            val posts = app.dao.galaxyPost.readTopPosts(userId)
            val galaxies = app.dao.galaxy.readTopGalaxies()
            val content = HomeContent(
                galaxies = galaxies,
                posts = posts,
            )
            call.respondHtml {
                homePage(content, SiteStyles)
            }
        }

        get("/g/{id}") {
            val pathId = call.parameters["id"] ?: return@get
            val galaxy = app.dao.galaxy.readGalaxyByPath(pathId) ?: return@get
            val userId = getUserIdOrNull()
            val posts = app.dao.galaxyPost.readPosts(galaxy.galaxyId, userId)
            val galaxies = app.dao.galaxy.readTopGalaxies()

            val content = GalaxyProfileContent(
                galaxy = galaxy,
                posts = posts,
                galaxies = galaxies,
            )

            call.respondHtml {
                galaxyProfilePage(content, SiteStyles)
            }
        }
    }
}

val uploadFolder = File("../upload")
val wwwFolder = File("../www")
