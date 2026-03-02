package streetlight.server.routes

import streetlight.agent.UrlParser
import streetlight.model.data.EventParse
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationParse
import streetlight.model.data.MultiEventParse
import streetlight.model.data.MultiEventParseResponse
import streetlight.model.data.ParseRequest
import streetlight.model.data.toEdit
import streetlight.model.data.toEventEdit
import streetlight.server.ServerProvider

class EventUrlReader(
    private val app: ServerProvider
) {
    private val agent = UrlParser(app.env.read("GEMINI_KEY_A"))

    suspend fun serve(request: ParseRequest): MultiEventParseResponse {
        val url = request.url
        val parse: MultiEventParse? = agent.readHtml(url, ReaderText.multiEventInstructions)
        val events = parse?.events?.mapNotNull { it.toEventEdit(url, null, null) }
        println(events?.size)
        return MultiEventParseResponse(
            hasContent = parse?.hasContent,
            events = events
        )
    }
}