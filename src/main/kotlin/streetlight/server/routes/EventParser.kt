package streetlight.server.routes

import kampfire.api.toMarkdown
import kampfire.model.Outcome
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toUrl
import koala.toImage
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
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

suspend fun DataScope.parseEvent(request: ParseRequest): Outcome<EventEdit> {
    log("parsing event")
    val html = when (request) {
        is UrlParseRequest -> fetchHtml(request.url)
        is HtmlParseRequest -> request.html
        is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
    } ?: return Problem("Unable to access the website.")

    val url = request.url

    val doc = parseDocument(html, request.url) ?: return Problem("Address did not serve HTML.")

    val meta = doc.readHtmlMetaInfo()
    val metaDescription by lazy { meta.description?.stripHtml()?.toMarkdown() }

    return when (val response = client.parser.readHtml<EventParse>(url, doc, ParserText.singleEventInstructions)) {
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