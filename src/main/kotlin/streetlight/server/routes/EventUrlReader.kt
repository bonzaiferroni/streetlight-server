package streetlight.server.routes

import streetlight.agent.UrlParser
import streetlight.model.data.EventParse
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationParse
import streetlight.model.data.MultiEventParse
import streetlight.model.data.MultiEventParseResponse
import streetlight.model.data.ParseRequest
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEdit
import streetlight.model.data.toEventEdit
import streetlight.server.ServerProvider

class EventUrlReader(
    private val app: ServerProvider
) {
    private val agent = UrlParser(app.env.read("GEMINI_KEY_B"))

    suspend fun serve(request: ParseRequest): MultiEventParseResponse {
        val parse: MultiEventParse? = when (request) {
            is UrlParseRequest -> agent.readUrl(request.url, ReaderText.multiEventInstructions)
            is HtmlParseRequest -> agent.readHtml(request.url, request.html, ReaderText.multiEventInstructions)
            is ImageParseRequest -> TODO()
        }
        val events = parse?.events?.mapNotNull { it.toEventEdit(request.url, null, null) }
        return MultiEventParseResponse(
            hasContent = parse?.hasContent,
            events = events
        )
    }
}