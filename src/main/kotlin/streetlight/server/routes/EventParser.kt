package streetlight.server.routes

import kampfire.api.toMarkdown
import kampfire.model.Outcome
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toDataOr
import kampfire.model.toDataOrNull
import koala.toImage
import streetlight.agent.HtmlParserClient
import streetlight.agent.fetchText
import streetlight.agent.parseHtmlDocument
import streetlight.model.data.EventEdit
import streetlight.model.data.EventParse
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.ParseRequest
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEventEdit
import streetlight.server.model.DataScope
import streetlight.server.utils.readHtmlMetaInfo
import streetlight.server.utils.stripHtml

/**
 * Reads an event from a web page, filling its image, description, and title from the page's meta tags when the
 * parse lacks them.
 */
suspend fun DataScope.parseEvent(request: ParseRequest, parser: HtmlParserClient): Outcome<EventEdit> {
    log.info { "parsing event" }
    val html = when (request) {
        is UrlParseRequest -> fetchText(request.url).toDataOrNull()?.text
        is HtmlParseRequest -> request.html
        is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
    } ?: return Problem("Unable to access the website.")

    val url = request.url

    val doc = parseHtmlDocument(html, request.url).toDataOr { return Problem("Address did not serve HTML.") }

    val meta = doc.readHtmlMetaInfo()
    val metaDescription by lazy { meta.description?.stripHtml()?.toMarkdown() }

    return when (val response = parser.readHtml(url, doc, ParserText.singleEventInstructions, EventParse::class)) {
        is Ok -> {
            val parse = response.data
            Ok(parse.toEventEdit(null).copy(
                image = meta.image?.toImage() ?: parse.imageUrl?.toImage(),
                description = parse.description?.toMarkdown() ?: metaDescription,
                title = parse.name ?: meta.title
            ))
        }
        is Problem -> {
            Ok(
                data = EventEdit(
                    title = meta.title,
                    description = metaDescription,
                    image = meta.image?.toImage()
                ),
                message = "${response.message} Returning only document meta information."
            )
        }
    }
}