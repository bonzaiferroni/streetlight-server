package streetlight.server.routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kabinet.console.globalConsole
import klutch.server.ApiContext
import koala.html.IdOrNullParse
import koala.html.IdParse
import koala.html.StaticParse
import kotlinx.html.HTML
import streetlight.model.data.StarPost
import streetlight.model.data.EventId
import streetlight.model.data.LocationId
import streetlight.server.model.*
import streetlight.server.SiteStyles
import streetlight.web.StreetlightScreen
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree
import streetlight.web.pages.*
import streetlight.web.shells.*
import java.io.File

private val console = globalConsole.getHandle(ApiContext::servePages.name)

fun ApiContext.servePages() {

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



    suspend fun renderScreen(screen: StreetlightScreen, arg: String?): HtmlRender? {
        return when (screen) {
            StreetlightScreen.Home -> renderHome()
            StreetlightScreen.AboutApp -> renderAboutApp()
            StreetlightScreen.Location -> renderLocation(arg)
            StreetlightScreen.Galaxy -> renderGalaxy(arg)
            StreetlightScreen.Star -> renderStar(arg)
            StreetlightScreen.EventProfile -> renderEventProfile(arg)
            StreetlightScreen.SiteDoc -> renderSiteDoc(arg)
            StreetlightScreen.Post -> renderPost(arg)
            else -> renderClientBase()
        }
    }

    StreetlightScreen.entries.forEach { screen ->

        val path = when (val parse = screen.routeParse) {
            is IdOrNullParse -> "/${screen.pathRoot}/{${parse.label}?}"
            is IdParse -> "/${screen.pathRoot}/{${parse.label}}"
            is StaticParse -> screen.pathRoot
        }

        get(path) {
            val arg = when (val parse = screen.routeParse) {
                is IdOrNullParse -> call.parameters[parse.label]
                is IdParse -> call.parameters[parse.label]
                is StaticParse -> null
            }

            when (val render = renderScreen(screen, arg)) {
                null -> {
                    call.respondHtml {
                        // td: not found
                    }
                }
                else -> {
                    call.respondHtml {
                        render.block(this)
                    }
                }
            }
        }
    }

    // not yet implemented in web app
    get("/event-signup/{id}") {
        val eventId = call.parameters["id"]?.let { EventId(it) } ?: return@get
        val event = dao.event.readEvent(eventId) ?: return@get
        call.respondHtml {
            eventSignUp(event, SiteStyles)
        }
    }
}

val uploadFolder = File("../upload")
val wwwFolder = File("../www")

data class HtmlRender(
    val block: HTML.() -> Unit
)

suspend fun ApiContext.renderHome(): HtmlRender {
    val content = server.get<ContentService>().readHomeContent()

    return HtmlRender {
        homePage(content, SiteStyles)
    }
}

suspend fun ApiContext.renderAboutApp(): HtmlRender {
    return HtmlRender {
        aboutPage(SiteStyles)
    }
}

suspend fun ApiContext.renderLocation(arg: String?): HtmlRender? {
    val locationId = arg?.let { LocationId(it) } ?: return null
    val location = dao.location.readLocation(locationId) ?: return null

    return HtmlRender {
        locationPage(location, SiteStyles)
    }
}

suspend fun ApiContext.renderGalaxy(arg: String?): HtmlRender? {
    val id = arg ?: return null
    val galaxy = dao.galaxy.readGalaxy(id) ?: return null
    val galaxyId = galaxy.galaxyId
    val posts = dao.post.readActivePosts(galaxyId)

    val content = GalaxyContent(
        galaxy = galaxy,
        posts = posts,
    )

    return HtmlRender {
        galaxyProfilePage(content, SiteStyles)
    }
}

suspend fun ApiContext.renderStar(arg: String?): HtmlRender? {
    val username = arg ?: return null
    val userId = dao.star.readIdByUsername(username) ?: return null // td: serve not found content
    val star = dao.star.readByUsername(username) ?: return null
    val posts = dao.post.readStarPosts(userId)
    val content = StarProfileContent(
        star = star,
        posts = posts
    )

    return HtmlRender {
        starProfilePage(content, SiteStyles)
    }
}

suspend fun ApiContext.renderEventProfile(arg: String?): HtmlRender? {
    val slug = arg ?: return null
    val event = dao.event.readEventLocationBySlug(slug) ?: return null

    return HtmlRender {
        appPage("${event.title} | Streetlight", SiteStyles) {
            eventShell(event)
        }
    }
}

suspend fun ApiContext.renderSiteDoc(arg: String?): HtmlRender? {
    val docId = arg ?: return null
    val node = SiteDocTree.nodes[docId] ?: return null

    return HtmlRender {
        appPage("${node.doc.title} | Streetlight", SiteStyles) {
            siteDocShell(node, SiteDocTable)
        }
    }
}

suspend fun ApiContext.renderPost(arg: String?): HtmlRender? {
    val id = arg ?: return null
    val post = dao.post.readPost(id) as? StarPost ?: return null

    return HtmlRender {
        appPage("${post.title} by ${post.username ?: "Someone"} | Streetlight", SiteStyles) {
            starPostShell(post)
        }
    }
}

suspend fun ApiContext.renderClientBase(): HtmlRender {
    console.log("ey")
    return HtmlRender {
        appPage("Streetlight", SiteStyles) { }
    }
}