package streetlight.server.routes

import kabinet.clients.readImageUrl
import kampfire.model.ApiResponse
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toUrl
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
import streetlight.server.model.StreetlightServer
import streetlight.server.utils.readHtmlMetaInfo

class LocationParser(
    private val app: StreetlightServer
) {
    private val agent = app.ai.parser

    suspend fun parseLocation(request: ParseRequest): ApiResponse<LocationEdit> {
        val html = when (request) {
            is UrlParseRequest -> fetchHtml(request.url)
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
        } ?: return Problem("Unable to access the website.")

        val url = request.url

        val doc = parseDocument(html, request.url) ?: return Problem("Address did not serve HTML.")

        val meta = doc.readHtmlMetaInfo()

        return when (val response = agent.readHtml<LocationParse>(url, doc, ParserText.locationInstructions)) {
            is Ok -> {
                val parse = response.data
                Ok(parse.toEdit(null).copy(
                    imageRef = meta.image ?: parse.imageUrl?.toUrl(),
                    description = parse.description ?: meta.description,
                    name = parse.name ?: meta.title
                ))
            }
            is Problem -> {
                Ok(
                    data = LocationEdit(
                        name = meta.title,
                        description = meta.description,
                        imageRef = meta.image
                    ),
                    message = "${response.message} Returning only document meta information."
                )
            }
        }
    }
}