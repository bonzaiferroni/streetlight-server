package streetlight.server.html

import kotlinx.html.*
import streetlight.model.dto.EventInfo
import streetlight.model.Song
import streetlight.server.utilities.callFunction

fun HTML.eventPage(
    host: String,
    info: EventInfo,
    songs: List<Song>,
) {
    basePage("Event") {
        div("event-image") {
            img {
                src = info.event.imageUrl ?: "static/img/bridge.jpg"
            }
        }
        h1("event-title") {
            +"Luke @ ${info.location.name}"
        }
        div {
            h4("venmo") {
                strong { +"venmo: " }
                a {
                    href = "https://www.venmo.com/u/colfaxband"
                    +"@colfaxband"
                }

            }
            p {
                +"Howdy, thanks for stopping by! "
                +"You can request a song or sing with me, just look below to see my song list. "
                +"100% of your support goes toward the development of the streetlight app and community. "
                +"Let me know if you have any questions or feedback, and thank you for listening!"
            }
            article {
                div {
                    span("label-text") { +"now playing: "}
                    span { id = "now-playing" }
                }
                div {
                    span("label-text") { +"up next: " }
                    span { id = "requests" }
                }
            }
            h3 {
                +"Request a song"
            }
            settings()
            div("rows") {
                songs.forEach {
                    requestRow(info.event.id, it)
                }
            }
        }
        script {
            src = "static/js/event.js"
        }
        callFunction("init", "\"$host\"", info.event.id)
    }
}

fun FlowContent.settings() {
    // input { type = InputType.text; placeholder = "your name (optional)" }
    div("columns gap") {
        fieldSet {
            legend { +"Options" }
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
                +"I'll sing solo"
            }
        }
        fieldSet {
            input {
                id = "requester-name"
                type = InputType.text
                placeholder = "Your name (optional)"
            }
            input {
                id = "other-notes"
                type = InputType.text
                placeholder = "Other notes (optional)"
            }
        }
    }
}

fun FlowContent.requestRow(eventId: Int, song: Song) {
    article {
        div("row, request-row") {
            div("name") {
                div("property") {
                    +"song"
                }
                div("value") {
                    +song.name
                }
            }
            div("artist") {
                song.artist?.let {
                    div("property") {
                        +"artist"
                    }
                    div("value") {
                        +it
                    }
                }
            }
            button {
                id = "song-button-${song.id}"
                classes = setOf("btn", "btn-primary", "song-button")
                onClick = "postRequest(${song.id}, $eventId)"
                +"Request"
            }
        }
    }
}