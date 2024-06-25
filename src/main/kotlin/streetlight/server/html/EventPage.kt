package streetlight.server.html

import kotlinx.html.*
import streetlight.model.Performance
import streetlight.server.utilities.callFunction

fun HTML.eventPage(
    eventId: Int,
    performances: List<Performance>,
) {
    basePage("Event") {
        h1 {
            +"Event name"
        }
        div("request-list") {
            h5 {
                +"What song should I play next?"
            }
            p {
                +"requested: "
                span { id = "requests" }
            }
            performances.forEach {
                button {
                    id = "song-button-${it.id}"
                    classes = setOf("btn", "btn-primary", "song-button")
                    onClick = "makeRequest(${it.id}, $eventId)"
                    +it.name
                }
            }
        }
        p {
            +"Thank you for stopping by :) "
            +"My name is Luke and I'm working on an app for street performers. "
            +"You can follow me on my blog or support me through Patreon or venmo. "
        }
        script {
            src = "static/request.js"
        }
        callFunction("getRequests", eventId)
    }
}