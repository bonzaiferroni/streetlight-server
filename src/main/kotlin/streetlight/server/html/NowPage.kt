package streetlight.server.html

import kotlinx.html.*
import streetlight.model.dto.EventInfo
import streetlight.model.utils.toLocalDateTime

fun HTML.nowPage(events: List<EventInfo>) {
    basePage("Events") {
        h1 {
            +"Events"
        }
        events.forEach {
            p {
                +"${it.location.name}, ${it.area?.name}"
                br
                +"${it.event.timeStart.toLocalDateTime()} - ${it.event.hours} hours"
            }
        }
    }
}