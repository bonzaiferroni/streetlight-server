package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.api.toMarkdown
import kampfire.model.ApiResponse
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toUrl
import streetlight.agent.ParserClient
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.model.data.EventEdit
import streetlight.model.data.EventParse
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.ParseRequest
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEventEdit
import streetlight.server.utils.readHtmlMetaInfo
import streetlight.server.utils.stripHtml

private val console = globalConsole.getHandle(EventParser::class)

class EventParser(
    private val parser: ParserClient
) {
    suspend fun parseEvent(request: ParseRequest): ApiResponse<EventEdit> {
        val html = when (request) {
            is UrlParseRequest -> fetchHtml(request.url)
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
        } ?: return Problem("Unable to access the website.")

        val url = request.url

        val doc = parseDocument(html, request.url) ?: return Problem("Address did not serve HTML.")

        val meta = doc.readHtmlMetaInfo()
        val metaDescription by lazy { meta.description?.stripHtml()?.toMarkdown() }

        return when (val response = parser.readHtml<EventParse>(url, doc, ParserText.singleEventInstructions)) {
            is Ok -> {
                val parse = response.data
                Ok(parse.toEventEdit(null).copy(
                    imageRef = meta.image ?: parse.imageUrl?.toUrl(),
                    description = parse.description?.toMarkdown() ?: metaDescription,
                    title = parse.name ?: meta.title
                ))
            }
            is Problem -> {
                Ok(
                    data = EventEdit(
                        title = meta.title,
                        description = metaDescription,
                        imageRef = meta.image
                    ),
                    message = "${response.message} Returning only document meta information."
                )
            }
        }
    }
}