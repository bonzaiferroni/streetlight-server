package streetlight.server.utils

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kabinet.clients.readMetaContent
import kampfire.model.toUrl

/** The content of the first meta tag named by one of [propertyValues], in order. */
fun Document.readMetaContent(vararg propertyValues: String) = propertyValues.firstNotNullOfOrNull {
    this.selectFirst("meta[property=\"$it\"]")?.attribute("content")?.value
        ?: this.selectFirst("meta[name=\"$it\"]")?.attribute("content")?.value
}

/** The image a page declares in its meta tags, or null when it declares none with an absolute url. */
fun Document.readImageUrl() = this.readMetaContent("image", "og:image", "twitter:image")?.let {
    if (it.startsWith("//")) "https:$it" else it
}?.replace(" ", "%20")?.toUrl()?.takeIf { it.isAbsolute }