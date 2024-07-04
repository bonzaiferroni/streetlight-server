package streetlight.server.html

import kotlinx.html.*

fun HTML.basePage(
    title: String,
    headElements: (HEAD.() -> Unit)? = null,
    content: MAIN.() -> Unit,
) {
    head {
        title {
            +"$title | streetlight"
        }
        // link { rel = "stylesheet"; href = "static/css/layout.css" }
        // link { rel = "stylesheet"; href = "static/css/foxy.css" }
        // google font
        link { rel = "preconnect"; href = "https://fonts.googleapis.com" }
        link { rel = "preconnect"; href = "https://fonts.gstatic.com" }
        link { rel = "stylesheet"; href = "https://fonts.googleapis.com/css2?family=Teko:wght@300..700&display=swap" }
        link {
            rel = "stylesheet"
            href =
                "https://fonts.googleapis.com/css2?family=Roboto+Condensed:ital,wght@0,100..900;1,100..900&display=swap"
        }
        // link { rel = "stylesheet"; href = "static/css/tailwind.css" }
        link {
            rel = "stylesheet"
            href = "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"
        }
        link {
            rel = "stylesheet"
            href = "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.colors.min.css"
        }
        link { rel = "stylesheet"; href = "static/css/styles.css" }
        // <meta name="viewport" content="width=device-width, initial-scale=1.0">
        meta("viewport", "width=device-width, initial-scale=1.0")
        headElements?.invoke(this)
    }
    body {
        header("site-header") {
            div("site-title") { +"streetlight" }
            div(classes = "logo") {
                img(src = "static/img/logo.png", alt = "Streetlight Logo")
            }
            nav {
                ul {
                    li { a(href = "/about") { +"About" } }
                    li { a(href = "https://eosris.ing") { +"Blog" } }
                    // github logo
                    li {
                        a(href = "https://github.com/bonzaiferroni/streetlight") {
                            img(classes = "logo-small", src = "static/img/github.svg", alt = "GitHub Logo")
                        }
                    }
                }
            }
        }

        main("container") {
            content()
        }
        footer { }
    }
}