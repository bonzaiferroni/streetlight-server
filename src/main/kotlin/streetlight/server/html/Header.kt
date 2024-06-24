package streetlight.server.html

import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.title

fun HTML.pageHeader(
    title: String,
    elements: (HEAD.() -> Unit)? = null
) {
    head {
        title {
            +"$title | streetlight"
        }
        link {
            rel = "stylesheet"
            href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
        }
        link {
            rel = "stylesheet"
            href = "static/styles.css"
        }
        link {
            rel = "stylesheet"
            href = "static/foxy.css"
        }
        elements?.invoke(this)
    }
}