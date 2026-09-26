package streetlight.server.db.datascope

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.toDataOr
import kampfire.model.toUrl
import streetlight.agent.EmptySchemaStore
import streetlight.agent.HtmlParserClient
import streetlight.agent.SchemaMediator
import streetlight.agent.fetchText
import streetlight.agent.parseHtmlDocument
import streetlight.agent.tryQuery
import streetlight.model.data.SelectorSchema
import streetlight.server.db.services.tryOutcome
import streetlight.server.model.DataScope

/**
 * Finds the selectors of the events at [url]: those of the feed, and those of an event's own page when the feed
 * links to one.
 *
 * The feed schema alone is returned, with a reason, when the event page cannot be read.
 */
suspend fun DataScope.parseEventSchema(url: Url, koog: HtmlParserClient): Outcome<List<SelectorSchema>> = tryOutcome {
    val mediator = SchemaMediator(koog, isRefining = false)
    val html = fetchText(url).toDataOr { return@tryOutcome it }.text

    val doc = parseHtmlDocument(html, url).toDataOr { return@tryOutcome it }

    val feedSchema = mediator.feedSchema(url, doc, EmptySchemaStore, timeZoneId = null).toDataOr {
        return@tryOutcome Problem("${it.message} Title: ${doc.title()}")
    }

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
    val pageSchema = when (val outcome = mediator.pageSchema(pageUrl, pageDoc, EmptySchemaStore, timeZoneId = null)) {
        is Problem -> return@tryOutcome Ok(feedOnlySchema, "${outcome.message} Title: ${pageDoc.title()}")
        is Ok -> outcome.data
    }

    if (pageSchema.title == null && feedSchema.title == null)
        return@tryOutcome Problem("No title selector present in either schema")
    Ok(listOf(feedSchema, pageSchema))
}
