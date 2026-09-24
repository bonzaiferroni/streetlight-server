package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kabinet.utils.Environment
import kampfire.api.ActionResult
import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.server.authGate
import klutch.server.provide
import koala.PageResource
import koala.html.AppScreen
import koala.html.SlugOrNullParse
import koala.html.IdParse
import koala.html.SegmentParse
import koala.html.SlugParse
import koala.html.StaticParse
import koala.html.UsernameParse
import koala.html.UuidParse
import kotlinx.html.HTML
import streetlight.model.data.AuthTokenType
import streetlight.server.model.*
import streetlight.model.ui.Screen
import streetlight.server.ServerResource
import streetlight.server.buildMode
import streetlight.web.pages.*
import streetlight.web.shells.*
import java.io.File

private val console = KotlinLogging.logger("server") //globalConsole.getHandle(ApiScope::servePages.name)

/**
 * Serves the HTML of each screen, rendered on the server when its screen has a renderer and as the bare client
 * otherwise.
 */
fun ApiScope.servePages(resource: PageResource) {
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

    suspend fun renderScreen(screen: Screen, arg: String?, caller: Identity?, call: ApplicationCall): HtmlRender? {
        return when (screen) {
            Screen.Home -> renderHome(caller?.callerId, resource)
            Screen.AboutApp -> renderAboutApp(resource)
            Screen.Location -> renderLocation(arg, caller, resource)
            Screen.CityList -> renderCityList(resource)
            Screen.City -> renderCity(arg, caller?.callerId, resource)
            Screen.Galaxy -> renderGalaxy(arg, caller?.callerId, resource)
            Screen.Star -> renderStar(arg, caller, resource)
            Screen.Event -> renderEvent(arg, caller?.callerId, resource)
            Screen.Docs -> renderSiteDoc(arg, resource)
            Screen.Media -> renderMedia(arg, resource)
            Screen.VerifyEmail -> renderTokenPage(arg, AuthTokenType.EmailVerification, resource)
            Screen.AccountNotOwned -> renderTokenPage(arg, AuthTokenType.AccountNotOwned, resource)
            Screen.PasswordReset -> renderTokenPage(arg, AuthTokenType.PasswordReset, resource)
            Screen.AccountLockdown -> renderTokenPage(arg, AuthTokenType.AccountLockdown, resource)
            Screen.ActionReport -> renderActionReport(call, arg, resource)
            else -> renderClientBase(screen, resource)
        }
    }

    authGate(optional = true) {
        Screen.entries.forEach { screen ->

            val paths = when (val parse = screen.routeParse) {
                is SlugParse -> listOf("${screen.pathBase}/{${parse.label}?}")
                is UsernameParse -> listOf("${screen.pathBase}/{${parse.label}?}")
                is SlugOrNullParse -> listOf("${screen.pathBase}/{${parse.label}?}")
                is IdParse -> listOf("${screen.pathBase}/{${parse.label}}")
                is UuidParse -> listOf("${screen.pathBase}/{${parse.label}}")
                is StaticParse -> listOf(screen.pathBase)
                is SegmentParse -> parse.roots.map { "${screen.pathBase}/$it/{id?}" } + screen.pathBase
            }

            paths.forEach { path ->
                get(path) {
                    val identity = call.getIdentityOrNull()
                    val arg = when (val parse = screen.routeParse) {
                        is StaticParse -> null
                        else -> call.parameters[parse.label]
                    }

                    when (val render = renderScreen(screen, arg, identity, call)) {
                        null -> {
                            call.respondHtml {
                                renderNotFound(resource).block(this)
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

        get("{...}") {
            call.respondHtml {
                renderNotFound(resource).block(this)
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

/** The app page without server-rendered content, for the client to fill. */
suspend fun ApiScope.renderClientBase(screen: AppScreen, resource: PageResource): HtmlRender {
    return HtmlRender {
        // td: add loading message
        // td: pass screen title
        appPage("Streetlight", screen, resource)
    }
}

suspend fun ApiScope.renderHome(callerId: CallerId?, resource: PageResource): HtmlRender {
    // console.log(callerId)
    val content = readHomeContent(callerId)

    return HtmlRender {
        appPage("Home", Screen.Galaxy, resource) {
            homeShell(content)
        }
    }
}

suspend fun ApiScope.renderCityList(resource: PageResource): HtmlRender {
    val content = readCityListContent()

    return HtmlRender {
        appPage("Cities", Screen.CityList, resource) {
            cityListShell(content)
        }
    }
}

suspend fun ApiScope.renderCity(arg: String?, callerId: CallerId?, resource: PageResource): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val content = readCityContent(slug, callerId) ?: return null

    return HtmlRender {
        appPage(content.city.name, Screen.City, resource) {
            cityShell(content)
        }
    }
}

fun ApiScope.renderNotFound(resource: PageResource): HtmlRender {
    return HtmlRender {
        appPage("Oops", Screen.Error, resource) {
            +"We didn't find it :("
        }
    }
}

suspend fun ApiScope.renderAboutApp(resource: PageResource): HtmlRender {
    return HtmlRender {
        aboutPage(resource)
    }
}

suspend fun ApiScope.renderLocation(arg: String?, caller: Identity?, resource: PageResource): HtmlRender? {
    val locationId = arg?.toSlug() ?: return null
    val content = readLocationContent(locationId, caller) ?: return null

    return HtmlRender {
        appPage(content.location.name ?: "Location", Screen.Location, resource, content.design?.theme) {
            locationShell(content)
        }
    }
}

suspend fun ApiScope.renderGalaxy(arg: String?, callerId: CallerId?, resource: PageResource): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val content = readGalaxyContent(slug, callerId) ?: return null

    return HtmlRender {
        appPage(content.galaxy.name, Screen.Galaxy, resource, content.design?.theme) {
            galaxyShell(content)
        }
    }
}

suspend fun ApiScope.renderStar(arg: String?, caller: Identity?, resource: PageResource): HtmlRender? {
    val username = arg?.toUsername() ?: return null
    val content = readStarContent(username, caller) ?: return null

    return HtmlRender {
        appPage(username.value, Screen.Star, resource, content.star.design?.theme) {
            starShell(content)
        }
    }
}

suspend fun ApiScope.renderEvent(arg: String?, callerId: CallerId?, resource: PageResource): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val event = dao.event.readEventLocationBySlug(slug, callerId) ?: return null

    return HtmlRender {
        appPage(event.title, Screen.Event, resource) {
            eventShell(event)
        }
    }
}

fun ApiScope.renderSiteDoc(arg: String?, resource: PageResource): HtmlRender? {
    val docId = arg ?: return null
    val content = readDocContent(docId) ?: return null

    return HtmlRender {
        appPage(content.node.doc.title, Screen.Docs, resource) {
            siteDocShell(content)
        }
    }
}

suspend fun ApiScope.renderMedia(arg: String?, resource: PageResource): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val media = dao.media.readMedia(slug) ?: return null

    return HtmlRender {
        appPage("${media.title} by ${media.username}", Screen.Media, resource, media.design?.theme) {
            mediaShell(media)
        }
    }
}

fun renderActionReport(call: ApplicationCall, arg: String?, resource: PageResource): HtmlRender {
    val result = ActionResult.of(arg)
    val title = when (result) {
        ActionResult.Success -> "Success"
        else -> "Oops"
    }
    val message = when(result) {
        ActionResult.Invalid -> "The action was not successful."
        ActionResult.Problem -> {
            call.readCookieMessage(Screen.ActionReport.pathBase) ?: "Something went wrong on our end."
        }
        ActionResult.Success -> "Success! You may close this tab."
    }
    return HtmlRender {
        messagePage(title, message, resource)
    }
}