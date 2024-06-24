package streetlight.server.html

import kotlinx.html.*
import streetlight.dto.RequestInfo
import streetlight.model.Performance
import streetlight.server.utilities.callFunction

fun HTML.requestPage(
    eventId: Int,
    performances: List<Performance>,
) {
    pageHeader("Event")
    body("container") {
        h1 {
            +"Request a song"
        }
        p {
            +"Thank you for stopping by :) "
            +"My name is Luke and I'm working on an app for street performers. "
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
                    classes = setOf("btn", "btn-primary")
                    onClick = "makeRequest(${it.id}, $eventId)"
                    +it.name
                }
            }
        }
        p {
            +"You can follow me on my blog or support me through Patreon or venmo. "
        }
        script {
            src = "static/request.js"
        }
        callFunction("getRequests", eventId)
    }
}