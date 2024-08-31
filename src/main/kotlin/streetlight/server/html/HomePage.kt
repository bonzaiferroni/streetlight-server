package streetlight.server.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.h1
import kotlinx.html.onClick
import streetlight.server.utilities.callFunction

fun HTML.homePage() {
    basePage("Home") {
        h1 {
            +"Hello world!"
        }
        button {
            onClick = "alert('Hello, world!')"
            +"Click me"
        }
        callFunction("refreshEvent", 1)
    }
}