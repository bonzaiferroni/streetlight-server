package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import kampfire.api.isValid
import kampfire.api.toSlug
import kampfire.model.CoreProblem
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toOutcome
import klutch.server.authGate
import klutch.server.postApi
import klutch.server.getApi
import koala.PageResource
import streetlight.model.Api
import streetlight.model.data.SubdomainConfig
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.utils.subdomain
import streetlight.server.service.readLocationContentBySubdomain
import streetlight.model.ui.LocationAppScreen
import streetlight.web.page.locationAppPage
import streetlight.web.shell.locationAppShell
import streetlight.web.shell.locationAppEventShell
import streetlight.model.ui.Screen
import streetlight.server.utils.respondBaseHostRedirect
import kampfire.api.Slug

fun ApiScope.serveSubdomains(resource: PageResource) {
    authGate(optional = true) {
        subdomain("streetlight.ing", "localhost") {
            get("/") {
                val slug = call.parameters.getOrFail("subdomain").toSlug()
                val render = renderLocationApp(slug, resource) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondHtml {
                    render.block(this)
                }
            }

            Screen.Event.toRoutePaths().forEach { path ->
                get(path) {
                    val slug = call.parameters.getOrFail("subdomain").toSlug()
                    val eventSlug = call.parameters[Screen.Event.routeParse.label]?.toSlug()
                        ?: return@get call.respondBaseHostRedirect()
                    val render = renderLocationAppEvent(slug, eventSlug, resource)
                        ?: return@get call.respondBaseHostRedirect()
                    call.respondHtml {
                        render.block(this)
                    }
                }
            }

            // the base app answers every path the LocationApp does not
            Screen.entries.filter { it != Screen.Home && it != Screen.Event }.forEach { screen ->
                screen.toRoutePaths().forEach { path ->
                    get(path) { call.respondBaseHostRedirect() }
                }
            }
        }
    }

    getApi(Api.Locations.ReadSubdomainContent) {
        readLocationContentBySubdomain(it.data, null).toOutcome()
    }

    authGate {
        postApi(Api.Locations.CheckSubdomain) {
            val identity = call.getIdentity()
            if (!identity.isAdmin && !dao.location.isHost(it.data.locationId, identity.callerId)) {
                return@postApi HttpProblem.NotAuthorized
            }
            when (val problem = subdomainProblemOrNull(it.data)) {
                null -> Ok(true)
                HttpProblem.Conflict -> Ok(false)
                else -> problem
            }
        }

        postApi(Api.Locations.UpdateSubdomain) {
            val identity = call.getIdentity()
            val config = it.data
            if (!identity.isAdmin && !dao.location.isHost(config.locationId, identity.callerId)) {
                return@postApi HttpProblem.NotAuthorized
            }
            subdomainProblemOrNull(config)?.let { problem -> return@postApi problem }
            when (val slug = config.slug) {
                null -> {
                    if (dao.subdomain.delete(config.locationId) == 1) Ok(Unit)
                    else HttpProblem.NotFound
                }
                else -> {
                    if (dao.subdomain.upsert(config.locationId, slug) == 1) Ok(Unit)
                    else HttpProblem.Conflict
                }
            }
        }
    }
}

private suspend fun ApiScope.subdomainProblemOrNull(config: SubdomainConfig): Problem? {
    val slug = config.slug ?: return null
    return when {
        !slug.isValid() -> CoreProblem.InvalidSlug
        ReservedSubdomain.isReserved(slug.value) -> HttpProblem.Conflict
        !dao.subdomain.isAvailable(slug, config.locationId) -> HttpProblem.Conflict
        else -> null
    }
}

/** The home of the LocationApp at the subdomain [slug], or `null` when no location holds it. Its visitors are not signed in. */
suspend fun ApiScope.renderLocationApp(slug: Slug, resource: PageResource): HtmlRender? {
    val content = readLocationContentBySubdomain(slug, null) ?: return null

    return HtmlRender {
        locationAppPage(content.location.name ?: "Location", LocationAppScreen.Home, resource, content.design?.theme) {
            locationAppShell(content)
        }
    }
}

/**
 * The event [eventSlug] in the LocationApp at the subdomain [slug], in the location's theme, or `null` when the event
 * is not at the subdomain's location.
 */
suspend fun ApiScope.renderLocationAppEvent(slug: Slug, eventSlug: Slug, resource: PageResource): HtmlRender? {
    val locationSlug = dao.subdomain.readLocationSlug(slug) ?: return null
    val event = dao.event.readEventLocationBySlug(eventSlug, null) ?: return null
    if (event.locationSlug != locationSlug) return null
    val design = dao.location.readDesign(locationSlug, null)?.design

    return HtmlRender {
        locationAppPage(event.title, Screen.Event, resource, design?.theme) {
            locationAppEventShell(event)
        }
    }
}

/** Subdomains a location cannot take. */
object ReservedSubdomain {
    /** Infrastructure and standard service names — collisions here break tooling. */
    private val infrastructure = setOf(
        "www", "api", "app", "cdn", "static", "assets", "media", "img", "images",
        "mail", "smtp", "imap", "pop", "ns", "ns1", "ns2", "mx", "dns",
        "ftp", "ssh", "vpn", "proxy", "gateway", "host", "server", "localhost",
        "dev", "staging", "test", "demo", "sandbox", "preview", "beta", "alpha",
        "admin", "root", "system", "internal", "private", "secure", "auth", "login",
        "db", "database", "cache", "queue", "socket", "ws", "wss", "sse",
        "status", "metrics", "health", "logs", "monitor", "grafana",
        "git", "ci", "build", "deploy", "docker", "registry"
    )

    /** RFC 2142 mailbox names — reserved by convention for role addresses. */
    private val roleAddresses = setOf(
        "postmaster", "hostmaster", "webmaster", "abuse", "noc", "security",
        "info", "support", "sales", "marketing", "noreply", "no-reply"
    )

    /** Streetlight's own vocabulary — these must never belong to one location. */
    private val platform = setOf(
        "streetlight", "galaxy", "galaxies", "star", "stars", "beacon", "beacons",
        "scout", "scouts", "light", "lights", "seed", "quorum", "portal",
        "map", "maps", "event", "events", "location", "locations", "post", "posts"
    )

    /** Institutional pages Streetlight will want for itself. */
    private val institutional = setOf(
        "about", "help", "docs", "blog", "news", "press", "contact", "legal",
        "privacy", "terms", "policy", "policies", "conduct", "safety", "trust",
        "donate", "give", "support-us", "jobs", "careers", "volunteer",
        "account", "settings", "profile", "signup", "register", "signin", "logout",
        "search", "explore", "discover", "feed", "home", "index"
    )

    /** Names that could be mistaken for Streetlight itself — impersonation risk. */
    private val impersonation = setOf(
        "official", "verified", "staff", "team", "moderator", "mod", "bot"
    )

    private val all = infrastructure + roleAddresses + platform + institutional + impersonation

    fun isReserved(slug: String) = slug.lowercase() in all
}