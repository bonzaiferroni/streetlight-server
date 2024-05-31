package streetlight.server.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.htmlObject
import kotlinx.html.onClick
import kotlinx.html.title

fun HTML.homePage() {
    pageHeader("Home")
    body {
        h1 {
            +"Hello world!"
        }
        button {
            +"Click me"
            onClick = "alert('Hello, world!')"
        }
    }
}