package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kampfire.api.toSlug
import kampfire.api.toUsername
import kampfire.model.CallerId
import kampfire.model.Identity
import klutch.server.authGate
import koala.html.AppScreen
import koala.html.SlugOrNullParse
import koala.html.IdParse
import koala.html.SegmentParse
import koala.html.SlugParse
import koala.html.StaticParse
import koala.html.UsernameParse
import koala.html.UuidParse
import kotlinx.html.HTML
import streetlight.server.model.*
import streetlight.server.SiteStyles
import streetlight.model.ui.Screen
import streetlight.web.doc.SiteDocTable
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

    suspend fun renderScreen(screen: Screen, arg: String?, caller: Identity?): HtmlRender? {
        return when (screen) {
            Screen.Home -> renderHome(caller?.callerId)
            Screen.AboutApp -> renderAboutApp()
            Screen.Location -> renderLocation(arg, caller)
            Screen.Galaxy -> renderGalaxy(arg, caller?.callerId)
            Screen.Star -> renderStar(arg, caller)
            Screen.Event -> renderEvent(arg, caller?.callerId)
            Screen.Docs -> renderSiteDoc(arg)
            Screen.Media -> renderMedia(arg)
            else -> renderClientBase(screen)
        }
    }

    authGate(optional = true) {
        Screen.entries.forEach { screen ->

            val paths = when (val parse = screen.routeParse) {
                is SlugParse -> listOf("/${screen.pathRoot}/{${parse.label}?}")
                is UsernameParse -> listOf("/${screen.pathRoot}/{${parse.label}?}")
                is SlugOrNullParse -> listOf("/${screen.pathRoot}/{${parse.label}?}")
                is IdParse -> listOf("/${screen.pathRoot}/{${parse.label}}")
                is UuidParse -> listOf("/${screen.pathRoot}/{${parse.label}}")
                is StaticParse -> listOf("/${screen.pathRoot}")
                is SegmentParse -> parse.roots.map { "/${screen.pathRoot}/$it/{id?}" }
            }

            paths.forEach { path ->
                get(path) {
                    val identity = call.getIdentityOrNull()
                    val arg = when (val parse = screen.routeParse) {
                        is StaticParse -> null
                        else -> call.parameters[parse.label]
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

suspend fun ApiScope.renderHome(callerId: CallerId?): HtmlRender {
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

suspend fun ApiScope.renderLocation(arg: String?, caller: Identity?): HtmlRender? {
    val locationId = arg?.toSlug() ?: return null
    val location = readLocationContent(locationId, caller) ?: return null

    return HtmlRender {
        locationPage(location, SiteStyles)
    }
}

suspend fun ApiScope.renderGalaxy(arg: String?, callerId: CallerId?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val content = readGalaxyContent(slug, callerId) ?: return null

    return HtmlRender {
        galaxyPage(content, SiteStyles)
    }
}

suspend fun ApiScope.renderStar(arg: String?, caller: Identity?): HtmlRender? {
    val username = arg?.toUsername() ?: return null
    val content = readStarContent(username, caller) ?: return null

    return HtmlRender {
        appPage("$username", SiteStyles, Screen.Star) {
            starShell(content)
        }
    }
}

suspend fun ApiScope.renderEvent(arg: String?, callerId: CallerId?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val event = dao.event.readEventLocationBySlug(slug, callerId) ?: return null

    return HtmlRender {
        appPage("${event.title} | Streetlight", SiteStyles, Screen.Event) {
            eventShell(event)
        }
    }
}

fun ApiScope.renderSiteDoc(arg: String?): HtmlRender? {
    val docId = arg ?: return null
    val content = readDocContent(docId) ?: return null

    return HtmlRender {
        appPage("${content.node.doc.title} | Streetlight", SiteStyles, Screen.Docs) {
            siteDocShell(content)
        }
    }
}

suspend fun ApiScope.renderMedia(arg: String?): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val post = dao.media.readMedia(slug) ?: return null

    return HtmlRender {
        appPage("${post.title} by ${post.username} | Streetlight", SiteStyles, Screen.Media) {
            mediaShell(post)
        }
    }
}

suspend fun ApiScope.renderClientBase(screen: AppScreen): HtmlRender {
    return HtmlRender {
        // td: add loading message
        appPage("Streetlight", SiteStyles, screen) { }
    }
}