package streetlight.server.html

import kotlinx.html.*
import streetlight.model.dto.EventInfo
import streetlight.model.Event
import streetlight.model.Performance
import streetlight.server.utilities.callFunction

fun HTML.eventPage(
    host: String,
    event: EventInfo,
    performances: List<Performance>,
) {
    basePage("Event") {
        img {
            src = "static/img/bridge.jpg"
        }
        h1("event-title") {
            +"Luke @ ${event.locationName}"
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
                +"up next: "
                span { id = "requests" }
            }
            h3 {
                +"Requests"
            }
            settings()
            div("rows") {
                performances.forEach {
                    requestRow(event.id, it)
                }
            }
        }
        script {
            src = "static/js/request.js"
        }
        callFunction("init", "\"$host\"", event.id)
    }
}

fun FlowContent.settings() {
    // input { type = InputType.text; placeholder = "your name (optional)" }
    div("columns gap") {
        fieldSet {
            legend { +"Singers" }
            label {
                input {
                    id = "luke-sings"
                    type = InputType.radio
                    name = "voice"
                    checked = true
                }
                +"Luke sings"
            }
            label {
                input {
                    id = "duet"
                    type = InputType.radio
                    name = "voice"
                }
                +"Duet"
            }
            label {
                input {
                    id = "requester-sings"
                    type = InputType.radio
                    name = "voice"
                }
                +"I'll sing"
            }
        }
        fieldSet {
            legend { +"Guitar" }
            label {
                input {
                    id = "luke-guitar"
                    type = InputType.checkBox
                    name = "luke-guitar"
                    checked = true
                }
                +"Luke plays guitar"
            }
            legend { +"You" }
            input {
                id = "requester-name"
                type = InputType.text
                placeholder = "Your name (optional)"
            }
        }
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
                onClick = "postRequest(${performance.id}, $eventId)"
                +"Request"
            }
        }
    }
}