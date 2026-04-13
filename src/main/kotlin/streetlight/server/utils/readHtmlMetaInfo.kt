package streetlight.server.utils

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kabinet.clients.readMetaContent
import kampfire.model.Url
import kampfire.model.toUrl

fun Document.readHtmlMetaInfo() = MetaInfo(
    title = readTitle(),
    description = readDescription(),
    image = readImageUrl()
)

data class MetaInfo(
    val title: String?,
    val description: String?,
    val image: Url?,
)

fun Document.readMetaContent(vararg propertyValues: String) = propertyValues.firstNotNullOfOrNull {
    this.selectFirst("meta[property=\"$it\"]")?.attribute("content")?.value
        ?: this.selectFirst("meta[name=\"$it\"]")?.attribute("content")?.value
}

fun Document.readTitle() = this.readMetaContent("title", "og:title", "twitter:title")
fun Document.readDescription() = this.readMetaContent("description", "og:description", "twitter:description")
fun Document.readImageUrl() = this.readMetaContent("image", "og:image", "twitter:image")?.toUrl()