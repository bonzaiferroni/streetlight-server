package streetlight.server.model

import com.fleeksoft.ksoup.nodes.Document
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import streetlight.agent.HtmlParserClient
import streetlight.agent.HtmlParseObserver
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class TestHtmlParserClient: HtmlParserClient {

    override suspend fun <T: Any> readHtml(
        url: Url,
        doc: Document,
        instructions: String,
        type: KClass<T>,
    ): Outcome<T> = readHtml(url, doc, instructions, type.createType())

    override suspend fun <T: Any> readHtml(
        url: Url,
        doc: Document,
        instructions: String,
        type: KType,
        retryCount: Int,
        observer: HtmlParseObserver?,
    ): Outcome<T> = Problem("No parse result is configured for $type")

    override suspend fun <T> readImage(url: String, instructions: String, type: KType): T? = null
}
