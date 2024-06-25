package streetlight.server.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.h1
import kotlinx.html.onClick

fun HTML.homePage() {
    basePage("Home") {
        h1 {
            +"Hello world!"
        }
        button {
            +"Click me"
            onClick = "alert('Hello, world!')"
        }
    }
}