package streetlight.server.html

import kotlinx.html.*
import streetlight.model.Performance
import streetlight.server.utilities.callFunction

fun HTML.eventPage(
    host: String,
    eventId: Int,
    performances: List<Performance>,
) {
    basePage("Event") {
        h1 {
            +"artist name @ location"
        }
        div {
            h5 {
                +"You can "
                a {
                    href = "https://www.venmo.com/u/colfaxband"
                    +"send a tip with venmo"
                }
                +" ❤"
            }
            p {
                +"requested: "
                span { id = "requests" }
            }
            div("rows") {
                performances.forEach {
                    requestRow(eventId, it)
                }
            }
        }
        script {
            src = "static/js/request.js"
        }
        callFunction("init", "\"$host\"", eventId)
    }
}

fun FlowContent.requestRow(eventId: Int, performance: Performance) {
    article {
        div("row, request-row") {
            div("name") {
                div("property") {
                    +"song"
                }
                div("value") {
                    +performance.name
                }
            }
            div("artist") {
                performance.artist?.let {
                    div("property") {
                        +"artist"
                    }
                    div("value") {
                        +it
                    }
                }
            }
            button {
                id = "song-button-${performance.id}"
                classes = setOf("btn", "btn-primary", "song-button")
                onClick = "makeRequest(${performance.id}, $eventId)"
                +"Request"
            }
        }
    }
}