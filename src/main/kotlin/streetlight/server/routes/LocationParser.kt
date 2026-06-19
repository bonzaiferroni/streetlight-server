package streetlight.server.routes

import kabinet.clients.readImageUrl
import kampfire.api.toMarkdown
import kampfire.model.ApiResponse
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toUrl
import streetlight.agent.ParserService
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.model.data.EventParseResult
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationParse
import streetlight.model.data.ParseRequest
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEdit
import streetlight.server.utils.readHtmlMetaInfo
import streetlight.server.utils.stripHtml

class LocationParser(
    private val parser: ParserService
) {
    suspend fun parseLocation(request: ParseRequest): ApiResponse<LocationEdit> {
        val html = when (request) {
            is UrlParseRequest -> fetchHtml(request.url)
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
        } ?: return Problem("Unable to access the website.")

        val url = request.url

        val doc = parseDocument(html, request.url) ?: return Problem("Address did not serve HTML.")

        val meta = doc.readHtmlMetaInfo()
        val metaDescription by lazy { meta.description?.stripHtml()?.toMarkdown() }

        return when (val response = parser.readHtml<LocationParse>(url, doc, ParserText.locationInstructions)) {
            is Ok -> {
                val parse = response.data
                Ok(parse.toEdit(null).copy(
                    imageRef = meta.image ?: parse.imageUrl?.toUrl(),
                    description = parse.description ?: metaDescription,
                    name = parse.name ?: meta.title
                ))
            }
            is Problem -> {
                Ok(
                    data = LocationEdit(
                        name = meta.title,
                        description = metaDescription,
                        imageRef = meta.image
                    ),
                    message = "${response.message} Returning only document meta information."
                )
            }
        }
    }
}