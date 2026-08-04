package streetlight.server.daemon

import com.fleeksoft.ksoup.nodes.Document
import io.ktor.server.application.Application
import kampfire.api.toMarkdown
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toUrl
import koala.Image
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.agent.tryQuery
import streetlight.model.data.EventEdit
import streetlight.model.data.EventPageSchema
import streetlight.model.data.LocationConfigContent
import streetlight.model.data.LocationId
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

//class ParseDaemon(private val dao: DaoFacade) {
//
//    suspend fun start() {
//        val contents = dao.location.readParsable()
//        contents.forEach {
//            parseLocationEvents(it)
//        }
//
////        var lastRun: Instant
////        while (true) {
////            lastRun = Clock.System.now()
////
////            delay(1.minutes - (Clock.System.now() - lastRun))
////        }
//    }
//
//    suspend fun parseLocationEvents(content: LocationConfigContent) {
//        val location = content.location
//        content.parseResult?.let { lastResult ->
//            if (!lastResult.isSuccess) return
//        }
//        content.parsedAt?.let { parsedAt ->
//            if (parsedAt > Clock.System.now() - 24.hours) return
//        }
//        val feedUrl = location.eventsUrl ?: error("url not found")
//        println("fetching feed: $feedUrl")
//        val feedHtml = fetchHtml(feedUrl) ?: error("could not fetch html")
//        val doc = parseDocument(feedHtml, feedUrl) ?: error("doc not found")
//        val schema = content.config.eventSchema ?: error("schema not found")
//        val feed = schema.feed.firstOrNull() ?: error("feed selector not found")
//        val eventSelector = feed.event ?: error("event selector not found")
//        val pageElementsFromFeed = when (val outcome = doc.body().tryQuery(eventSelector)) {
//            is Problem -> error(outcome.message)
//            is Ok -> outcome.data
//        }
//
//        val gathered = pageElementsFromFeed.map { element ->
//            val feedDescription = element.queryElement(feed.description)
//            val feedEvent = RawEvent(
//                title = element.queryElement(feed.title).plainText(),
//                image = element.queryElement(feed.image).absoluteUrl("src"),
//                descriptionHtml = feedDescription.takeIf { it.isPlausibleProse() }.innerHtml(),
//                cost = element.queryElement(feed.cost).plainText(),
//                date = element.queryElement(feed.date).plainText(),
//                startTime = element.queryElement(feed.time).plainText(),
//            )
//            val pageUrl = element.queryElement(feed.link).absoluteUrl("href")?.toUrl()
//
//            val pageEvent = pageUrl?.let { url ->
//                println("fetching page: $url")
//                val pageHtml = fetchHtml(url) ?: return@let null
//                delay(1.minutes)
//                val pageDoc = parseDocument(pageHtml, url) ?: return@let null
//                val pageSchema = schema.page.firstOrNull() ?: return@let null
//                parsePageEvent(pageSchema, pageDoc)
//            }
//
//            RawEvent(
//                title = pageEvent?.title ?: feedEvent.title,
//                image = pageEvent?.image ?: feedEvent.image,
//                descriptionHtml = pageEvent?.descriptionHtml ?: feedEvent.descriptionHtml,
//                contact = pageEvent?.contact,
//                cost = pageEvent?.cost ?: feedEvent.cost,
//                ageMin = pageEvent?.ageMin,
//                date = pageEvent?.date ?: feedEvent.date,
//                startTime = pageEvent?.startTime ?: feedEvent.startTime,
//                endTime = pageEvent?.endTime,
//            )
//        }
//
//
//    }
//
//    private fun parsePageEvent(schema: EventPageSchema, doc: Document): RawEvent {
//        val body = doc.body()
//        return RawEvent(
//            title = body.queryElement(schema.title).takeIf { it.isPlausibleField() }.plainText(),
//            image = body.queryElement(schema.image).absoluteUrl("src"),
//            descriptionHtml = body.queryElement(schema.description).takeIf { it.isPlausibleProse() }.innerHtml(),
//            contact = body.queryElement(schema.contact).takeIf { it.isPlausibleField() }.plainText(),
//            cost = body.queryElement(schema.cost).takeIf { it.isPlausibleField() }.plainText(),
//            ageMin = body.queryElement(schema.ageMin).takeIf { it.isPlausibleField() }.plainText(),
//            date = body.queryElement(schema.date).takeIf { it.isPlausibleField() }.plainText(),
//            startTime = body.queryElement(schema.startTime).takeIf { it.isPlausibleField() }.plainText(),
//            endTime = body.queryElement(schema.endTime).takeIf { it.isPlausibleField() }.plainText(),
//        )
//    }
//}

fun Application.startParseDaemon(server: Server) {
    launch {
        // ParseDaemon(server.dao).start()
    }
}

data class RawEvent(
    val title: String? = null,
    val image: String? = null,
    val descriptionHtml: String? = null,
    val contact: String? = null,
    val cost: String? = null,
    val ageMin: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

private fun RawEvent.toEventEdit(website: Url?, timeZoneId: String?, locationId: LocationId): EventEdit {

    val dateTimeText = listOfNotNull(date, startTime.takeIf { it != date })
        .joinToString(" ")
        .takeIf { it.isNotBlank() }

    val description = descriptionHtml?.let { htmlToMarkdown(it) }
    val start = dateTimeText?.let { parseLocalDateTime(it, timeZoneId) }
    val end = endTime?.let { parseTimeFromText(it) }

    val notes = listOfNotNull(
        endTime?.let { "* **Ends:** $it" }, // td: escape markdown
        cost?.let { "* **Cost:** $it" },
        ageMin?.let { "* **Ages:** $it" },
    )
    val timeNote = if (start == null) (startTime ?: date)?.let { "**Time:** $it" } else null

    val body = listOfNotNull(
        timeNote,
        description,
        notes.joinToString("\n").takeIf { it.isNotBlank() },
    ).joinToString("\n\n").takeIf { it.isNotBlank() }

    return EventEdit(
        locationId = locationId,
        title = title,
        description = body?.toMarkdown(),
        contact = contact, // td: gather phone/email/social media separately
        website = website,
        image = image?.let { Image(it.toUrl()) },
        date = start?.date,
        startTime = start?.time,
        endTime = end,
        timeZoneId = timeZoneId,
        // td: parse ageMin
    )
}

//         val chromeFields = setOfNotNull(
//            "descriptionHtml".takeIf { gathered.isConstant { event -> event.descriptionHtml } },
//            "title".takeIf { gathered.isConstant { event -> event.title } },
//            "date".takeIf { gathered.isConstant { event -> event.date } },
//        )
//        if (chromeFields.isNotEmpty()) println("suspected chrome, identical across feed: $chromeFields")
//
//        gathered.forEach { raw ->
//            val judged = raw.copy(
//                descriptionHtml = raw.descriptionHtml.takeIf { "descriptionHtml" !in chromeFields },
//                title = raw.title.takeIf { "title" !in chromeFields },
//                date = raw.date.takeIf { "date" !in chromeFields },
//            )
//            val edit = judged.toEventEdit(null, location.timezoneId, location.locationId)
//            println(prettyPrint(edit))
//        }