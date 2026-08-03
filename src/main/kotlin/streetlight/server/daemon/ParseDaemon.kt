package streetlight.server.daemon

import com.fleeksoft.ksoup.nodes.Element
import io.ktor.server.application.Application
import kampfire.api.Markdown
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toUrl
import koala.Image
import koala.utils.prettyPrint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.agent.tryQuery
import streetlight.model.data.EventEdit
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ParseDaemon(private val dao: DaoFacade) {

    suspend fun parseLocations() {
        val contents = dao.location.readParsable()
        contents.forEach { content ->
            content.parseResult?.let { lastResult ->
                if (!lastResult.isSuccess) return@forEach
            }
            content.parsedAt?.let { parsedAt ->
                if (parsedAt > Clock.System.now() - 24.hours) return@forEach
            }
            val feedUrl = content.location.eventsUrl ?: error("url not found")
            println("fetching feed: $feedUrl")
            val feedHtml = fetchHtml(feedUrl) ?: error("could not fetch html")
            val doc = parseDocument(feedHtml, feedUrl) ?: error("doc not found")
            val schema = content.config.eventSchema ?: error("schema not found")
            val eventSelector = schema.feed.event ?: error("event selector not found")
            val elements = when (val outcome = doc.body().tryQuery(eventSelector)) {
                is Problem -> error(outcome.message)
                is Ok -> outcome.data
            }
            elements.forEach { element ->
                val feed = schema.feed
                val feedEvent = RawEvent(
                    title = element.queryText(feed.title),
                    image = element.queryUrl(feed.image, "src"),
                    description = element.queryText(feed.description),
                    cost = element.queryText(feed.cost),
                    date = element.queryText(feed.date),
                    startTime = element.queryText(feed.time),
                )
                val pageUrl = element.queryUrl(feed.link, "href")?.toUrl()

                val pageEvent = pageUrl?.let { url ->
                    val pageSchema = schema.page ?: return@let null
                    println("fetching page: $url")
                    val pageHtml = fetchHtml(url) ?: error("could not fetch event page: ${url.value}")
                    val pageDoc = parseDocument(pageHtml, url) ?: error("event page did not serve html")
                    val body = pageDoc.body()
                    RawEvent(
                        title = body.queryText(pageSchema.title),
                        image = body.queryUrl(pageSchema.image, "src"),
                        description = body.queryText(pageSchema.description),
                        contact = body.queryText(pageSchema.contact),
                        cost = body.queryText(pageSchema.cost),
                        ageMin = body.queryText(pageSchema.ageMin),
                        date = body.queryText(pageSchema.date),
                        startTime = body.queryText(pageSchema.startTime),
                        endTime = body.queryText(pageSchema.endTime),
                    ).also { delay(1.minutes) }
                }

                val merged = RawEvent(
                    title = pageEvent?.title ?: feedEvent.title,
                    image = pageEvent?.image ?: feedEvent.image,
                    description = pageEvent?.description ?: feedEvent.description,
                    contact = pageEvent?.contact,
                    cost = pageEvent?.cost ?: feedEvent.cost,
                    ageMin = pageEvent?.ageMin,
                    date = pageEvent?.date ?: feedEvent.date,
                    startTime = pageEvent?.startTime ?: feedEvent.startTime,
                    endTime = pageEvent?.endTime,
                )

                println(prettyPrint(merged.toEventEdit(pageUrl)))
            }
        }
    }

    suspend fun start() {
        var lastRun: Instant
        while (true) {
            lastRun = Clock.System.now()

            parseLocations()

            delay(1.minutes - (Clock.System.now() - lastRun))
        }
    }
}

fun Application.startParseDaemon(server: Server) {
    launch {
        ParseDaemon(server.dao).parseLocations()
    }
}

private fun Element.queryText(selector: String?): String? = selector?.let { query ->
    when (val outcome = tryQuery(query)) {
        is Problem -> error(outcome.message)
        is Ok -> outcome.data.firstOrNull()?.text()?.takeIf { it.isNotBlank() }
    }
}

private fun Element.queryUrl(selector: String?, attribute: String): String? = selector?.let { query ->
    when (val outcome = tryQuery(query)) {
        is Problem -> error(outcome.message)
        is Ok -> outcome.data.firstOrNull()?.absUrl(attribute)?.takeIf { it.isNotBlank() }
    }
}

private class RawEvent(
    val title: String? = null,
    val image: String? = null,
    val description: String? = null,
    val contact: String? = null,
    val cost: String? = null,
    val ageMin: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

private fun RawEvent.toEventEdit(website: Url?): EventEdit {
    val notes = listOfNotNull(
        date?.let { "Date: $it" },
        startTime?.let { "Start: $it" },
        endTime?.let { "End: $it" },
        cost?.let { "Cost: $it" },
        ageMin?.let { "Ages: $it" },
    )
    val body = (listOfNotNull(description) + notes)
        .joinToString("\n\n")
        .takeIf { it.isNotBlank() }

    return EventEdit(
        title = title,
        description = body?.let { Markdown(it) },
        contact = contact,
        website = website,
        image = image?.let { Image(it.toUrl()) },
    )
}