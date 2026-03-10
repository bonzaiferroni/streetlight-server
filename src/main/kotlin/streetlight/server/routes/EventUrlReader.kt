package streetlight.server.routes

import kabinet.console.globalConsole
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.MultiEventParse
import streetlight.model.data.MultiEventParseResponse
import streetlight.model.data.ParseRequest
import streetlight.model.data.SingleEventParse
import streetlight.model.data.SingleEventParseResponse
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEventEdit
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(EventUrlReader::class)

class EventUrlReader(
    private val app: ServerProvider
) {
    private val agent = app.parser

    suspend fun serveMulti(request: ParseRequest): MultiEventParseResponse {
        val instructions = ParserText.multiEventInstructions
        val parse: MultiEventParse? = when (request) {
            is UrlParseRequest -> agent.readUrl(request.url, instructions)
            is HtmlParseRequest -> agent.readHtml(request.url, request.html, instructions)
            is ImageParseRequest -> TODO()
        }
        if (parse == null) {
            console.log("Parse was null")
        }
        val events = parse?.events?.map { it.toEventEdit(request.url, null, null) }
        return MultiEventParseResponse(
            hasContent = parse?.hasContent,
            events = events
        )
    }

    suspend fun serveSingle(request: ParseRequest): SingleEventParseResponse {
        val instructions = ParserText.singleEventInstructions
        val parse: SingleEventParse? = when (request) {
            is UrlParseRequest -> agent.readUrl(request.url, instructions)
            is HtmlParseRequest -> agent.readHtml(request.url, request.html, instructions)
            is ImageParseRequest -> TODO()
        }
        if (parse == null) {
            console.log("Parse was null")
        }
        val event = parse?.event?.toEventEdit(request.url, null, null)
        return SingleEventParseResponse(
            hasContent = parse?.hasContent,
            event = event
        )
    }
}