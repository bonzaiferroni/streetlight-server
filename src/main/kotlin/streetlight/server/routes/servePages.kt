package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.server.authGate
import koala.html.SlugOrNullParse
import koala.html.IdParse
import koala.html.SlugParse
import koala.html.StaticParse
import koala.html.UuidParse
import kotlinx.html.HTML
import streetlight.model.data.GalaxyContent
import streetlight.model.data.StarId
import streetlight.server.model.*
import streetlight.server.SiteStyles
import streetlight.web.StreetlightScreen
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree
import streetlight.web.pages.*
import streetlight.web.shells.*
import java.io.File

private val console = KotlinLogging.logger("server") //globalConsole.getHandle(ApiScope::servePages.name)

fun ApiScope.servePages() {

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

    suspend fun renderScreen(screen: StreetlightScreen, arg: String?, caller: StarIdentity?): HtmlRender? {
        return when (screen) {
            StreetlightScreen.Home -> renderHome(caller?.starId)
            StreetlightScreen.AboutApp -> renderAboutApp()
            StreetlightScreen.Location -> renderLocation(arg, caller)
            StreetlightScreen.Galaxy -> renderGalaxy(arg, caller?.starId)
            StreetlightScreen.Star -> renderStar(arg, caller?.starId)
            StreetlightScreen.Event -> renderEventProfile(arg, caller?.starId)
            StreetlightScreen.Docs -> renderSiteDoc(arg)
            StreetlightScreen.Media -> renderMedia(arg)
            else -> renderClientBase()
        }
    }

    authGate(optional = true) {
        StreetlightScreen.entries.forEach { screen ->

            val path = when (val parse = screen.routeParse) {
                is SlugParse -> "/${screen.pathRoot}/{${parse.label}?}"
                is SlugOrNullParse -> "/${screen.pathRoot}/{${parse.label}?}"
                is IdParse -> "/${screen.pathRoot}/{${parse.label}}"
                is UuidParse -> "/${screen.pathRoot}/{${parse.label}}"
                is StaticParse -> screen.pathRoot
            }

            get(path) {
                val identity = call.getIdentityOrNull()
                val arg = when (val parse = screen.routeParse) {
                    is SlugParse -> call.parameters[parse.label]
                    is SlugOrNullParse -> call.parameters[parse.label]
                    is IdParse -> call.parameters[parse.label]
                    is UuidParse -> call.parameters[parse.label]
                    is StaticParse -> null
                }

                when (val render = renderScreen(screen, arg, identity)) {
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
    }

    // not yet implemented in web app
    get("/event-signup/{id}") {
//        val eventId: EventId = call.parameters["id"]?.toRecordId() ?: return@get
//        val event = dao.event.readEvent(eventId) ?: return@get
//        call.respondHtml {
//            eventSignUp(event, SiteStyles)
//        }
    }
}

val uploadFolder = File("../upload")
val wwwFolder = File("../www")

data class HtmlRender(
    val block: HTML.() -> Unit
)

suspend fun ApiScope.renderHome(callerId: StarId?): HtmlRender {
    // console.log(callerId)
    val content = readHomeContent(callerId)

    return HtmlRender {
        homePage(content, SiteStyles)
    }
}

suspend fun ApiScope.renderAboutApp(): HtmlRender {
    return HtmlRender {
        aboutPage(SiteStyles)
    }
}

suspend fun ApiScope.renderLocation(arg: String?, caller: StarIdentity?): HtmlRender? {
    val locationId = arg?.toSlug() ?: return null
    val location = readLocationContent(locationId, caller) ?: return null

    return HtmlRender {
        locationPage(location, SiteStyles)
    }
}

suspend fun ApiScope.renderGalaxy(arg: String?, callerId: StarId?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val galaxy = dao.galaxy.readGalaxy(slug, callerId) ?: return null
    val galaxyId = galaxy.galaxyId
    val posts = dao.post.readOrderedPosts(galaxyId, callerId)

    val content = GalaxyContent(
        galaxy = galaxy,
        posts = posts,
    )

    return HtmlRender {
        galaxyProfilePage(content, SiteStyles)
    }
}

suspend fun ApiScope.renderStar(arg: String?, starId: StarId?): HtmlRender? {
    val username = arg?.toUsername() ?: return null
    val userId = dao.star.readIdByUsername(username) ?: return null // td: serve not found content
    val star = dao.star.readByUsername(username) ?: return null
    val posts = dao.post.readStarPosts(userId, starId)
    val content = StarProfileContent(
        star = star,
        posts = posts
    )

    return HtmlRender {
        starProfilePage(content, SiteStyles)
    }
}

suspend fun ApiScope.renderEventProfile(arg: String?, starId: StarId?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val event = dao.event.readEventLocationBySlug(slug, starId) ?: return null

    return HtmlRender {
        appPage("${event.title} | Streetlight", SiteStyles) {
            eventShell(event)
        }
    }
}

suspend fun ApiScope.renderSiteDoc(arg: String?): HtmlRender? {
    val docId = arg ?: return null
    val node = SiteDocTree.nodes[docId] ?: return null

    return HtmlRender {
        appPage("${node.doc.title} | Streetlight", SiteStyles) {
            siteDocShell(node, SiteDocTable)
        }
    }
}

suspend fun ApiScope.renderMedia(arg: String?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val post = dao.media.readMedia(slug) ?: return null

    return HtmlRender {
        appPage("${post.title} by ${post.username} | Streetlight", SiteStyles) {
            mediaShell(post)
        }
    }
}

suspend fun ApiScope.renderClientBase(): HtmlRender {
    return HtmlRender {
        appPage("Streetlight", SiteStyles) { }
    }
}