package streetlight.server.daemon

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import kampfire.api.toMarkdown
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toDataOr
import kampfire.model.toUrl
import klutch.server.provide
import koala.Image
import koala.utils.prettyPrint
import kotlinx.coroutines.launch
import streetlight.agent.AGENT_TOKEN
import streetlight.agent.KoogParserClient
import streetlight.agent.fetchText
import streetlight.agent.parseHtmlDocument
import streetlight.agent.tryQuery
import streetlight.model.data.EventEdit
import streetlight.model.data.EventFeedSchema
import streetlight.model.data.EventPageSchema
import streetlight.model.data.LocationId
import streetlight.model.data.Origin
import streetlight.model.data.OriginSchema
import streetlight.model.data.toOriginId
import streetlight.server.model.ContentParse
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import streetlight.server.plugins.logger
import streetlight.server.routes.SchemaParserText
import kotlin.time.Duration.Companion.hours

class ParseDaemon(private val dao: DaoFacade, private val koog: KoogParserClient) {

    suspend fun start() {
        val locations = dao.location.readCheckable(checkInterval - 1.hours)
        locations.forEach { location ->
            dao.location.updateCheckedAt(location.locationId)

            val feedUrl = requireNotNull(location.eventsUrl)
            val originId = feedUrl.toOriginId() ?: return@forEach
            if (originId.value != "swallowhillmusic.org") return@forEach

            val origin = dao.origin.readOrCreateOrigin(originId)
            val gate = origin.getRobotGate()

            val feedHtml = gate.fetchWhenOpen(feedUrl).toDataOr(::logProblem) { return@forEach }
            val feedDoc = parseHtmlDocument(feedHtml, feedUrl).toDataOr(::logProblem) { return@forEach }
            val feedSchema = origin.getFeedSchema(feedUrl, feedDoc).toDataOr(::logProblem) { return@forEach }

            val eventSelector = feedSchema.event ?: return@forEach
            val body = feedDoc.body()
            val pageElements = body.tryQuery(eventSelector).toDataOr(::logProblem) { return@forEach }
            val rawEvents = pageElements.mapNotNull { element ->
                val feedEvent = RawEvent(
                    title = element.queryElement(feedSchema.title).plainText(),
                    image = element.queryElement(feedSchema.image).absoluteUrl("src"),
                    descriptionHtml = element.queryElement(feedSchema.description)
                        .takeIf { it.isPlausibleProse() }.innerHtml(),
                    cost = element.queryElement(feedSchema.cost).plainText(),
                    date = element.queryElement(feedSchema.date).plainText(),
                    startTime = element.queryElement(feedSchema.time).plainText(),
                )
                val pageUrl = element.queryElement(feedSchema.link).absoluteUrl("href")?.toUrl()
                val pageEvent = pageUrl?.let { url ->
                    val pageHtml = gate.fetchWhenOpen(url).toDataOr(::logProblem) { return@let null }
                    val pageDoc = parseHtmlDocument(pageHtml, url).toDataOr(::logProblem) { return@let null }

                    val pageSchema = origin.getPageSchema(url, pageDoc).toDataOr(::logProblem) { return@let null }
                    parsePageEvent(pageSchema, pageDoc)
                }

                val event = RawEvent(
                    title = pageEvent?.title ?: feedEvent.title,
                    image = pageEvent?.image ?: feedEvent.image,
                    descriptionHtml = pageEvent?.descriptionHtml ?: feedEvent.descriptionHtml,
                    contact = pageEvent?.contact,
                    cost = pageEvent?.cost ?: feedEvent.cost,
                    ageMin = pageEvent?.ageMin,
                    date = pageEvent?.date ?: feedEvent.date,
                    startTime = pageEvent?.startTime ?: feedEvent.startTime,
                    endTime = pageEvent?.endTime,
                )

                event.toEventEdit(pageUrl, location.timezoneId, location.locationId).also {
                    println(prettyPrint(it))
                }
            }
        }
    }

    fun logProblem(problem: Problem) {
        logger.info { problem.message }
    }

    fun logProblem(message: String) = logProblem(Problem(message))

    private suspend fun Origin.getRobotGate(): RobotGate {
        robotsTxt?.let {
            return it.toRobotGate()
        }
        val txt = fetchText(originId.toRobotsTxtUrl()).toDataOrNull() ?: return RobotGate(null, AGENT_TOKEN)
        dao.origin.updateRobotsTxt(originId, txt)
        return txt.toRobotGate()
    }

    private suspend fun Origin.getFeedSchema(url: Url, doc: Document): Outcome<EventFeedSchema> {
        val body = doc.body()
        schemas.forEach { schema ->
            val content = schema.content as? EventFeedSchema ?: return@forEach
            val eventSelector = content.event ?: return@forEach
            val pageElements = body.tryQuery(eventSelector).toDataOr { return@forEach }
            val isSuccess = !pageElements.isEmpty()
            dao.origin.updateSchemaResult(schema.originSchemaId, isSuccess)
            if (isSuccess) return Ok(content)
        }

        // td: limit LM call by interval
        val content = koog.readHtml<ContentParse<EventFeedSchema>>(
            url = url, doc = doc, instructions = SchemaParserText.EventFeedSelectorsInstructions
        ).toDataOr { return it }

        val contentSchema = content.content
        if (!content.isExpectedContent || contentSchema == null) {
            return Problem("Document content was not an event feed")
        }

        dao.origin.create(originId, contentSchema)
        return Ok(contentSchema)
    }

    private suspend fun Origin.getPageSchema(url: Url, doc: Document): Outcome<EventPageSchema> {
        schemas.forEach { schema ->
            val content = schema.content as? EventPageSchema ?: return@forEach
            val pageEvent = parsePageEvent(content, doc)
            val isSuccess = !pageEvent.title.isNullOrBlank() && !pageEvent.descriptionHtml.isNullOrBlank()
            println(pageEvent.descriptionHtml?.length)
            dao.origin.updateSchemaResult(schema.originSchemaId, isSuccess)
            if (isSuccess) return Ok(content)
        }

        // td: likewise limit LM call by interval
        val content = koog.readHtml<ContentParse<EventPageSchema>>(
            url = url, doc = doc, instructions = SchemaParserText.EventPageSelectorsInstructions
        ).toDataOr { return it }

        val contentSchema = content.content
        if (!content.isExpectedContent || contentSchema == null) {
            return Problem("Document content was not an event page")
        }

        dao.origin.create(originId, contentSchema)
        return Ok(contentSchema)
    }


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

    private fun parsePageEvent(schema: EventPageSchema, doc: Document): RawEvent {
        val body = doc.body()
        return RawEvent(
            title = body.queryElement(schema.title).takeIf { it.isPlausibleField() }.plainText(),
            image = body.queryElement(schema.image).absoluteUrl("src"),
            descriptionHtml = body.queryElement(schema.description).takeIf { it.isPlausibleProse() }.innerHtml(),
            contact = body.queryElement(schema.contact).takeIf { it.isPlausibleField() }.plainText(),
            cost = body.queryElement(schema.cost).takeIf { it.isPlausibleField() }.plainText(),
            ageMin = body.queryElement(schema.ageMin).takeIf { it.isPlausibleField() }.plainText(),
            date = body.queryElement(schema.date).takeIf { it.isPlausibleField() }.plainText(),
            startTime = body.queryElement(schema.startTime).takeIf { it.isPlausibleField() }.plainText(),
            endTime = body.queryElement(schema.endTime).takeIf { it.isPlausibleField() }.plainText(),
        )
    }

    private class SchemaCache(initial: List<OriginSchema>) {
        private val schemas = initial.toMutableList()

        fun ordered(): List<OriginSchema> = schemas.sortedByDescending { it.lastSuccessAt }

        fun add(schema: OriginSchema) = schemas.add(schema)
    }
}

fun Application.startParseDaemon(server: Server) {
    launch {
        ParseDaemon(server.dao, server.provide<KoogParserClient>()).start()
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

private val checkInterval = 24.hours

private val logger = KotlinLogging.logger(ParseDaemon::class)

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