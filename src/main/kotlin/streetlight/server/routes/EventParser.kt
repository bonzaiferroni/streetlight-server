package streetlight.server.routes

import kabinet.clients.readImageUrl
import kabinet.console.globalConsole
import kampfire.model.toUrl
import streetlight.agent.fetchHtml
import streetlight.agent.parseDocument
import streetlight.model.data.EventEdit
import streetlight.model.data.EventParse
import streetlight.model.data.HtmlParseRequest
import streetlight.model.data.ImageParseRequest
import streetlight.model.data.ParseRequest
import streetlight.model.data.EventParseResult
import streetlight.model.data.UrlParseRequest
import streetlight.model.data.toEventEdit
import streetlight.server.model.StreetlightServer
import streetlight.server.utils.readHtmlMetaInfo

private val console = globalConsole.getHandle(EventParser::class)

class EventParser(
    private val app: StreetlightServer
) {
    private val agent = app.ai.parser

//    suspend fun readEvents(request: ParseRequest): MultiEventParseResponse {
//        val instructions = ParserText.multiEventInstructions
//        val result: ParserResult<MultiEventParse>? = when (request) {
//            is UrlParseRequest -> agent.readUrl(request.url, instructions)
//            is HtmlParseRequest -> agent.readHtml(request.url, request.html, instructions)
//            is ImageParseRequest -> TODO()
//        }
//        val parse = result?.value
//        if (parse == null) {
//            console.log("Parse was null")
//        }
//        val events = parse?.events?.map { it.toEventEdit(request.url, null, null) }
//        return MultiEventParseResponse(
//            hasContent = parse?.hasContent,
//            events = events
//        )
//    }

    suspend fun parseEvent(request: ParseRequest): EventParseResult {
        val instructions = ParserText.singleEventInstructions

        val html = when (request) {
            is UrlParseRequest -> fetchHtml(request.url)
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> TODO()
        } ?: return EventParseResult(false)

        val doc = parseDocument(html, request.url) ?: return EventParseResult(false)

        val parse: EventParse? = agent.readHtml(request.url, doc, instructions)

        val meta = doc.readHtmlMetaInfo()

        if (parse == null) {
            return EventParseResult(
                hasContent = true,
                event = EventEdit(
                    title = meta.title,
                    description = meta.description,
                    imageRef = meta.image
                )
            )
        }

        val featureImage = doc.readImageUrl()?.toUrl()
        val event = parse.toEventEdit(request.url, null, null).copy(
            imageRef = featureImage,
            description = parse.description ?: meta.description,
            title = parse.name ?: meta.title
        )

        return EventParseResult(
            hasContent = true,
            event = event
        )
    }
}