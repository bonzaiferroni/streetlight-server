package streetlight.server.routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.server.model.*
import streetlight.server.SiteStyles
import streetlight.server.model.StreetlightRouting
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree
import streetlight.web.pages.*
import streetlight.web.shells.*
import java.io.File

private val console = globalConsole.getHandle(StreetlightRouting::servePages.name)

fun StreetlightRouting.servePages() {

//    get("/event-portal/{id}") {
//        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
//        val event = app.dao.event.readEvent(eventId) ?: return@get
//        val spark = app.dao.spark.readByUserId(event.starId)
//        val requestItems = app.dao.song.readRequestItems(event.starId)
//        call.respondHtml {
//            console.log("responding")
//            eventPortal(event, spark, requestItems, SiteStyles)
//        }
//    }

    get("/") {
        val posts = server.dao.post.readActivePosts()
        val galaxies = server.dao.galaxy.readTopGalaxies()
        val content = HomeContent(
            galaxies = galaxies,
            posts = posts,
        )
        call.respondHtml {
            homePage(content, SiteStyles)
        }
    }

    get("/about") {
        call.respondHtml {
            aboutPage(SiteStyles)
        }
    }

    get("/event-signup/{id}") {
        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
        val event = server.dao.event.readEvent(eventId) ?: return@get
        call.respondHtml {
            eventSignUp(event, SiteStyles)
        }
    }

    get("/location/{id}") {
        val locationId = call.parameters["id"]?.let { LocationId(it) } ?: return@get
        val location = server.dao.location.readLocation(locationId) ?: return@get

        call.respondHtml {
            locationPage(location, SiteStyles)
        }
    }

    get("/g/{path}") {
        val path = call.parameters["path"] ?: return@get
        val galaxy = server.dao.galaxy.readGalaxyByPath(path) ?: return@get
        val galaxyId = galaxy.galaxyId
        val posts = server.dao.post.readActivePosts(galaxyId)

        val content = GalaxyProfileContent(
            galaxy = galaxy,
            posts = posts,
        )

        call.respondHtml {
            galaxyProfilePage(content, SiteStyles)
        }
    }

    get("/s/{username}") {
        val username = call.parameters["username"] ?: return@get
        val userId = server.dao.star.readIdByUsername(username) ?: return@get // td: serve not found content
        val star = server.dao.star.readByUsername(username) ?: error("star not found")
        val posts = server.dao.post.readStarPosts(userId)
        val content = StarProfileContent(
            star = star,
            posts = posts
        )

        call.respondHtml {
            starProfilePage(content, SiteStyles)
        }
    }

    get("/e/{slug}") {
        val slug = call.parameters["slug"] ?: return@get
        val event = server.dao.event.readEventLocationBySlug(slug) ?: return@get

        call.respondHtml {
            eventPage(event, SiteStyles)
        }
    }

    get("/docs/{docId}") {
        val docId = call.parameters["docId"] ?: return@get
        val node = SiteDocTree.nodes[docId] ?: return@get

        call.respondHtml {
            siteDocPage(node, SiteDocTable, SiteStyles)
        }
    }
}

val uploadFolder = File("../upload")
val wwwFolder = File("../www")
