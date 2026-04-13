package streetlight.server.routes

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

class LocationParser(
    private val app: StreetlightServer
) {
    private val agent = app.ai.parser

    suspend fun parseLocation(request: ParseRequest): LocationEdit? {
        val url = request.url

        val html = when (request) {
            is UrlParseRequest -> fetchHtml(request.url)
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> TODO()
        } ?: return null

        val doc = parseDocument(html, request.url) ?: return null

        val parse: LocationParse? = agent.readHtml(url, doc, ParserText.locationInstructions)
        return parse?.toEdit(null)
    }
}