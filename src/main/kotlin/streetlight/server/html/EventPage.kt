package streetlight.server.html

import kotlinx.html.*
import streetlight.dto.EventInfo
import streetlight.model.Event
import streetlight.model.Performance
import streetlight.server.utilities.callFunction

fun HTML.eventPage(
    host: String,
    event: EventInfo,
    performances: List<Performance>,
) {
    basePage("Event") {
        h1 {
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
                +"Make a request"
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
    div("columns") {
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
        }
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