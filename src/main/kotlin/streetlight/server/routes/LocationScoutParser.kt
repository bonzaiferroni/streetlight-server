package streetlight.server.routes

import streetlight.agent.ParserResult
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
        val parse: ParserResult<LocationParse>? = agent.readUrl(url, ParserText.locationInstructions)
        return parse?.value?.toEdit(null)
    }
}