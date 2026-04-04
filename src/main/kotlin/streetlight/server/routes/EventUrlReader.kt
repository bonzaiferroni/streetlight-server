package streetlight.server.routes

import kabinet.clients.readImageUrl
import kabinet.console.console
import streetlight.agent.ParserResult
import streetlight.model.data.EventParse
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.MultiEventParse
import streetlight.model.data.MultiEventParseResponse
import streetlight.model.data.ParseRequest
import streetlight.model.data.SingleEventParseResponse
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEventEdit
import streetlight.server.ServerProvider

private val console = console.getHandle(EventUrlReader::class)

class EventUrlReader(
    private val app: ServerProvider
) {
    private val agent = app.parser

    suspend fun serveMulti(request: ParseRequest): MultiEventParseResponse {
        val instructions = ParserText.multiEventInstructions
        val result: ParserResult<MultiEventParse>? = when (request) {
            is UrlParseRequest -> agent.readUrl(request.url, instructions)
            is HtmlParseRequest -> agent.readHtml(request.url, request.html, instructions)
            is ImageParseRequest -> TODO()
        }
        val parse = result?.value
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
        val result: ParserResult<EventParse>? = when (request) {
            is UrlParseRequest -> agent.readUrl(request.url, instructions)
            is HtmlParseRequest -> agent.readHtml(request.url, request.html, instructions)
            is ImageParseRequest -> TODO()
        }
        if (result == null) {
            console.log("Parse was null")
        }
        val parse = result?.value
        val featureImage = result?.document?.readImageUrl()
        console.log(result?.value?.cost)
        val event = parse?.toEventEdit(request.url, null, null)?.copy(imageUrl = featureImage)
        return SingleEventParseResponse(
            hasContent = result != null,
            event = event
        )
    }
}