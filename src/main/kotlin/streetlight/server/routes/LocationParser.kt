package streetlight.server.routes

import kampfire.api.toMarkdown
import kampfire.model.Outcome
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.toDataOr
import kampfire.model.toDataOrNull
import kampfire.model.toUrl
import koala.toImage
import streetlight.agent.KoogParserClient
import streetlight.agent.fetchText
import streetlight.agent.parseHtmlDocument
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
    private val parser: KoogParserClient
) {
    suspend fun parseLocation(request: ParseRequest): Outcome<LocationEdit> {
        val html = when (request) {
            is UrlParseRequest -> fetchText(request.url).toDataOrNull()?.text
            is HtmlParseRequest -> request.html
            is ImageParseRequest -> return Problem("Parsing images is not yet supported.")
        } ?: return Problem("Unable to access the website.")

        val url = request.url

        val doc = parseHtmlDocument(html, request.url).toDataOr { return Problem("Address did not serve HTML.") }

        val meta = doc.readHtmlMetaInfo()
        val metaDescription by lazy { meta.description?.stripHtml()?.toMarkdown() }

        return when (val response = parser.readHtml(url, doc, ParserText.locationInstructions, LocationParse::class)) {
            is Ok -> {
                val parse = response.data
                Ok(parse.toEdit(null).copy(
                    image = meta.image?.toImage() ?: parse.imageUrl?.toUrl()?.toImage(),
                    description = parse.description?.toMarkdown() ?: metaDescription,
                    name = parse.name ?: meta.title
                ))
            }
            is Problem -> {
                Ok(
                    data = LocationEdit(
                        name = meta.title,
                        description = metaDescription,
                        image = meta.image?.toImage()
                    ),
                    message = "${response.message} Returning only document meta information."
                )
            }
        }
    }
}