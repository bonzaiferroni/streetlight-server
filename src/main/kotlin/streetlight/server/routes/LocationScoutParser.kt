package streetlight.server.routes

import streetlight.agent.UrlParser
import streetlight.model.data.LocationEdit
import streetlight.model.data.LocationParse
import streetlight.model.data.ParseRequest
import streetlight.model.data.toEdit
import streetlight.server.ServerProvider

class LocationScoutParser(
    private val app: ServerProvider
) {
    private val agent = app.parser

    suspend fun serve(request: ParseRequest): LocationEdit? {
        val url = request.url
        val parse: LocationParse? = agent.readUrl(url, ReaderText.locationInstructions)
        return parse?.toEdit(null)
    }
}