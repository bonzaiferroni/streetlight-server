package streetlight.server.html

import kotlinx.html.*

fun HTML.basePage(
    title: String,
    headElements: (HEAD.() -> Unit)? = null,
    content: BODY.() -> Unit,
) {
    head {
        title {
            +"$title | streetlight"
        }
        link { rel = "stylesheet"; href = "static/css/tailwind.css" }
        // link { rel = "stylesheet"; href = "static/css/styles.css" }
        // link { rel = "stylesheet"; href = "static/css/layout.css" }
        // link { rel = "stylesheet"; href = "static/css/foxy.css" }
        // google font
        link { rel = "preconnect"; href = "https://fonts.googleapis.com" }
        link { rel = "preconnect"; href = "https://fonts.gstatic.com" }
        link { rel = "stylesheet"; href = "https://fonts.googleapis.com/css2?family=Teko:wght@300..700&display=swap" }
        link {
            rel = "stylesheet"
            href = "https://fonts.googleapis.com/css2?family=Roboto:wght@300..700&display=swap"
        }
        link {
            rel = "stylesheet"
            href =
                "https://fonts.googleapis.com/css2?family=Oswald:wght@200..700&family=Roboto+Condensed:ital,wght@0,100..900;1,100..900&display=swap"
        }
        headElements?.invoke(this)
    }
    body {
        content()
    }
}