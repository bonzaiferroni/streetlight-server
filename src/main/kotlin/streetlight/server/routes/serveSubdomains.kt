package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import kampfire.api.toSlug
import kampfire.model.HttpProblem
import kampfire.model.Ok
import klutch.server.authGate
import klutch.server.postApi
import koala.PageResource
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentityOrNull
import streetlight.server.utils.subdomain

fun ApiScope.serveSubdomains(resource: PageResource) {
    authGate(optional = true) {
        subdomain("streetlight.ing", "localhost") {
            get("/") {
                val slug = call.parameters.getOrFail("subdomain").toSlug()
                val locationSlug = dao.subdomain.readLocationSlug(slug) ?: return@get call.respond(HttpStatusCode.NotFound)
                val render = renderLocation(locationSlug.value, call.getIdentityOrNull(), resource) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondHtml {
                    render.block(this)
                }
            }
        }
    }

    authGate {
        postApi(Api.Locations.UpdateSubdomain) {
            call.requireAdminIdentity()
            val config = it.data
            when (val slug = config.slug) {
                null -> {
                    if (dao.subdomain.delete(config.locationId) == 1) Ok(Unit)
                    else HttpProblem.NotFound
                }
                else -> {
                    if (ReservedSubdomain.isReserved(slug.value)) HttpProblem.Conflict
                    else if (dao.subdomain.upsert(config.locationId, slug) == 1) Ok(Unit)
                    else HttpProblem.Conflict
                }
            }
        }
    }
}

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