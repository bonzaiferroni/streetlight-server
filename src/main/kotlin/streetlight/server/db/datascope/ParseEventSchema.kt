package streetlight.server.db.datascope

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toDataOr
import kampfire.model.toUrl
import streetlight.agent.KoogParserClient
import streetlight.agent.fetchText
import streetlight.agent.parseHtmlDocument
import streetlight.agent.tryQuery
import streetlight.model.data.SelectorSchema
import streetlight.model.data.EventFeedSchema
import streetlight.model.data.EventPageSchema
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.ContentParse
import streetlight.server.model.DataScope
import streetlight.server.routes.SchemaParserText

suspend fun DataScope.parseEventSchema(url: Url, koog: KoogParserClient): Outcome<List<SelectorSchema>> = tryOutcome {
    val html = fetchText(url).toDataOr { return@tryOutcome it }.text

    val doc = parseHtmlDocument(html, url).toDataOr { return@tryOutcome it }

    val eventContent = when (val outcome = koog.readHtml<ContentParse<EventFeedSchema>>(url, doc, SchemaParserText.EventFeedSelectorsInstructions)) {
        is Problem -> return@tryOutcome outcome
        is Ok -> outcome.data
    }

    val feedSchema = eventContent.content
    if (!eventContent.isExpectedContent || feedSchema == null)
        return@tryOutcome Problem("Unexpected feed content, title: ${doc.title()}")

    val eventSelector = feedSchema.event ?: return@tryOutcome Problem("No event selector found")
    val feedOnlySchema = listOf(feedSchema)
    val linkSelector = feedSchema.link ?: run {
        if (feedSchema.title != null) return@tryOutcome Ok(feedOnlySchema)
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
    val pageHtml = when (val outcome = fetchText(pageUrl)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, "Page fetch problem: ${outcome.message}")
        is Ok -> outcome.data.text
    }

    val pageDoc = parseHtmlDocument(pageHtml, pageUrl).toDataOr {
        return@tryOutcome Ok(feedOnlySchema, "Page url did not serve HTML")
    }
    val pageContent = when (val outcome = koog.readHtml<ContentParse<EventPageSchema>>(pageUrl, pageDoc, SchemaParserText.EventPageSelectorsInstructions)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, outcome.message)
        is Ok -> outcome.data
    }

    val pageSchema = pageContent.content
    if (!pageContent.isExpectedContent || pageSchema == null) {
        return@tryOutcome Ok(feedOnlySchema, "Unexpected page content, title: ${pageDoc.title()}")
    }

    if (pageSchema.title == null && feedSchema.title == null)
        return@tryOutcome Problem("No title selector present in either schema")
    Ok(listOf(feedSchema, pageSchema))
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