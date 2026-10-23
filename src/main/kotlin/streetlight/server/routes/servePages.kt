package streetlight.server.routes

import streetlight.server.service.readCityContent
import streetlight.server.service.readCityFeedContent
import streetlight.server.service.readDocContent
import streetlight.server.service.readGalaxyContent
import streetlight.server.service.readGalaxyFeedContent
import streetlight.server.service.readHomeContent
import streetlight.server.service.readLocationContent
import streetlight.server.service.readSkillContent
import streetlight.server.service.readStarContent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import kampfire.api.ActionResult
import kampfire.api.toSlug
import kampfire.api.toUsername
import klutch.db.model.CallerId
import klutch.db.model.Identity
import klutch.server.authGate
import koala.PageResource
import koala.model.AppScreen
import koala.model.SlugOrNullParse
import koala.model.IdParse
import koala.model.PathParse
import koala.model.SlugParse
import koala.model.StaticParse
import koala.model.UsernameParse
import koala.model.UuidParse
import kotlinx.html.HTML
import streetlight.model.data.AuthTokenType
import streetlight.model.data.OnboardType
import streetlight.model.data.FeedType
import streetlight.model.data.OfferingId
import streetlight.model.data.SkillId
import kotlin.uuid.Uuid
import streetlight.server.model.*
import streetlight.model.ui.Screen
import streetlight.model.ui.readConnectType
import streetlight.model.ui.readFeedType
import java.io.File
import streetlight.web.page.*
import streetlight.web.shell.*

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
            Screen.Home -> renderHome(caller?.callerId, call.request.queryParameters.readFeedType(), resource)
            Screen.AboutApp -> renderAboutApp(resource)
            Screen.Connect -> renderConnect(call.request.queryParameters.readConnectType() ?: OnboardType.BusinessOwner, resource)
            Screen.Location -> renderLocation(arg, caller, resource)
            Screen.CityFeed -> renderCityFeed(resource)
            Screen.GalaxyFeed -> renderGalaxyFeed(caller?.callerId, call.request.queryParameters.readFeedType(), resource)
            Screen.City -> renderCity(arg, caller?.callerId, call.request.queryParameters.readFeedType(), resource)
            Screen.Galaxy -> renderGalaxy(arg, caller?.callerId, resource)
            Screen.Star -> renderStar(arg, caller, resource)
            Screen.Event -> renderEvent(arg, caller?.callerId, resource)
            Screen.Docs -> renderSiteDoc(arg, resource)
            Screen.Media -> renderMedia(arg, resource)
            Screen.Offering -> renderOffering(arg, caller?.callerId, resource)
            Screen.SkillProfile -> renderSkill(arg, resource)
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
            screen.toRoutePaths().forEach { path ->
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

/** The routing paths that reach this screen, with its argument as a path parameter named by its parse. */
fun AppScreen.toRoutePaths() = when (val parse = routeParse) {
    is SlugParse -> listOf("$pathBase/{${parse.label}?}")
    is UsernameParse -> listOf("$pathBase/{${parse.label}?}")
    is SlugOrNullParse -> listOf("$pathBase/{${parse.label}?}")
    is IdParse -> listOf("$pathBase/{${parse.label}}")
    is UuidParse -> listOf("$pathBase/{${parse.label}}")
    is StaticParse -> listOf(pathBase)
    is PathParse -> parse.roots.map { "$pathBase/$it/{id?}" } + pathBase
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

suspend fun ApiScope.renderHome(callerId: CallerId?, feed: FeedType?, resource: PageResource): HtmlRender {
    val content = readHomeContent(callerId, feed)

    return HtmlRender {
        appPage("Home", Screen.Galaxy, resource) {
            homeShell(content)
        }
    }
}

suspend fun ApiScope.renderCityFeed(resource: PageResource): HtmlRender {
    val content = readCityFeedContent()

    return HtmlRender {
        appPage("Cities", Screen.CityFeed, resource) {
            cityFeedShell(content)
        }
    }
}

suspend fun ApiScope.renderGalaxyFeed(callerId: CallerId?, feed: FeedType?, resource: PageResource): HtmlRender {
    val content = readGalaxyFeedContent(callerId, feed)

    return HtmlRender {
        appPage("Galaxies", Screen.GalaxyFeed, resource) {
            galaxyFeedShell(content)
        }
    }
}

suspend fun ApiScope.renderCity(arg: String?, callerId: CallerId?, feed: FeedType?, resource: PageResource): HtmlRender? {
    val slug = arg?.toSlug() ?: return null
    val content = readCityContent(slug, callerId, feed) ?: return null

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

fun renderConnect(type: OnboardType, resource: PageResource): HtmlRender {
    return HtmlRender {
        appPage("Connect", Screen.Connect, resource) {
            connectShell(type)
        }
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

suspend fun ApiScope.renderOffering(arg: String?, callerId: CallerId?, resource: PageResource): HtmlRender? {
    val offeringId = arg?.let { Uuid.parseOrNull(it) }?.let { OfferingId(it) } ?: return null
    val offering = dao.offering.readOffering(offeringId, callerId) ?: return null

    return HtmlRender {
        appPage(offering.title, Screen.Offering, resource) {
            offeringShell(offering)
        }
    }
}

suspend fun ApiScope.renderSkill(arg: String?, resource: PageResource): HtmlRender? {
    val skillId = arg?.let { Uuid.parseOrNull(it) }?.let { SkillId(it) } ?: return null
    val content = readSkillContent(skillId) ?: return null

    return HtmlRender {
        appPage(content.skill.name, Screen.SkillProfile, resource) {
            skillShell(content)
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