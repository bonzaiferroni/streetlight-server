package streetlight.server.routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.model.data.PostListing
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider
import streetlight.server.SiteStyles
import streetlight.web.pages.eventPage
import streetlight.web.pages.eventPortal
import streetlight.web.pages.eventSignUp
import streetlight.web.pages.galaxyProfilePage
import streetlight.web.pages.homePage
import streetlight.web.pages.locationPage
import streetlight.web.pages.starProfilePage
import streetlight.web.shells.GalaxyProfileContent
import streetlight.web.shells.HomeContent
import streetlight.web.shells.StarProfileContent
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

    get("/location/{id}") {
        val locationId = call.parameters["id"]?.let { LocationId(it) } ?: return@get
        val location = app.dao.location.readLocation(locationId) ?: return@get

        call.respondHtml {
            locationPage(location, SiteStyles)
        }
    }

    get("/") {
        val posts = app.dao.eventPost.readTopPosts()
        val galaxies = app.dao.galaxy.readTopGalaxies()
        val content = HomeContent(
            galaxies = galaxies,
            posts = posts,
        )
        call.respondHtml {
            homePage(content, SiteStyles)
        }
    }

    get("/g/{path}") {
        val path = call.parameters["path"] ?: return@get
        val galaxy = app.dao.galaxy.readGalaxyByPath(path) ?: return@get
        val galaxyId = galaxy.galaxyId
        val events = app.dao.eventPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        val locations = app.dao.locationPost.readPosts(galaxyId).takeIf { it.isNotEmpty() }
        val listing = PostListing(
            events = events,
            locations = locations
        )

        val content = GalaxyProfileContent(
            galaxy = galaxy,
            listing = listing,
        )

        call.respondHtml {
            galaxyProfilePage(content, SiteStyles)
        }
    }

    get("/s/{username}") {
        val username = call.parameters["username"] ?: return@get
        val userId = app.dao.user.readIdByUsername(username) ?: return@get // td: serve not found content
        val star = app.dao.star.readByUsername(username) ?: error("star not found")
        val events = app.dao.eventPost.readPosts(userId).takeIf { it.isNotEmpty() }
        val locations = app.dao.locationPost.readPosts(userId).takeIf { it.isNotEmpty() }
        val listing = PostListing(
            events = events,
            locations = locations
        )
        val content = StarProfileContent(
            star = star,
            listing = listing
        )

        call.respondHtml {
            starProfilePage(content, SiteStyles)
        }
    }

    get("/e/{slug}") {
        val slug = call.parameters["slug"] ?: return@get
        val event = app.dao.event.readEventBySlug(slug) ?: return@get

        call.respondHtml {
            eventPage(event, SiteStyles)
        }
    }
}

val uploadFolder = File("../upload")
val wwwFolder = File("../www")
