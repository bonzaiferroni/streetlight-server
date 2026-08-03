package streetlight.server.db.datascope

import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.Selector
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toUrl
import streetlight.agent.KoogParserClient
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.agent.tryQuery
import streetlight.model.data.EventFeedSelectors
import streetlight.model.data.EventPageSelectors
import streetlight.model.data.EventSelectorSchema
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope
import streetlight.server.routes.ParserText

suspend fun DataScope.parseEventSchema(url: Url, koog: KoogParserClient): Outcome<EventSelectorSchema> = tryOutcome {
    val html = fetchHtml(url) ?: return@tryOutcome Problem("Unable to fetch HTML.")
    val doc = parseDocument(html, url) ?: return@tryOutcome Problem("Feed url did not serve HTML.")

    val feedSelector = when (val outcome = koog.readHtml(url, doc, ParserText.eventFeedSelectorsInstructions, EventFeedSelectors::class)) {
        is Problem -> return@tryOutcome outcome
        is Ok -> outcome.data
    }

    val eventSelector = feedSelector.event ?: return@tryOutcome Problem("No event selector found")
    val feedOnlySchema = EventSelectorSchema(feedSelector, null)
    val linkSelector = feedSelector.link ?: run {
        if (feedSelector.title != null) return@tryOutcome Ok(feedOnlySchema)
        return@tryOutcome Problem("Invalid feed-only schema: missing title")
    }
    val element = when (val outcome = doc.body().tryQuery(eventSelector)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, outcome.message)
        is Ok -> {
            outcome.data.firstOrNull() ?: return@tryOutcome Problem("No event elements found")
        }
    }

    val linkElement = when (val outcome = element.tryQuery(linkSelector)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, outcome.message)
        is Ok -> {
            outcome.data.firstOrNull() ?: return@tryOutcome Ok(feedOnlySchema, "No link elements found")
        }
    }

    val pageUrl = linkElement.attribute("href")?.value?.toUrl() ?: return@tryOutcome Ok(feedOnlySchema, "No link href found")
    val pageHtml = fetchHtml(pageUrl) ?: return@tryOutcome Ok(feedOnlySchema, "Unable to fetch event page")
    val pageDoc = parseDocument(pageHtml, pageUrl) ?: return@tryOutcome Ok(feedOnlySchema, "Page url did not serve HTML")
    when (val outcome = koog.readHtml(pageUrl, pageDoc, ParserText.eventPageSelectorsInstructions, EventPageSelectors::class)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, outcome.message)
        is Ok -> {
            val pageSelector = outcome.data
            if (pageSelector.title == null && feedSelector.title == null)
                return@tryOutcome Problem("No title selector present in either schema")
            Ok(EventSelectorSchema(feedSelector, outcome.data))
        }
    }
}

private fun String.titleTokens(): Set<String> =
    lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(" ")
        .filter { it.isNotBlank() }
        .toSet()

fun titlesOverlap(first: String, second: String, threshold: Float = 0.5f): Boolean {
    val a = first.titleTokens()
    val b = second.titleTokens()
    if (a.isEmpty() || b.isEmpty()) return false

    val shared = a.intersect(b).size
    return shared.toFloat() / minOf(a.size, b.size) > threshold
}