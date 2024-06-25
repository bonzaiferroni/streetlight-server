package streetlight.server.html

import kotlinx.html.*
import streetlight.dto.EventInfo
import streetlight.utils.toLocalDateTime

fun HTML.eventsPage(events: List<EventInfo>) {
    basePage("Events") {
        h1 {
            +"Events"
        }
        events.forEach {
            p {
                +"${it.locationName}, ${it.areaName}"
                br
                +"${it.timeStart.toLocalDateTime()} - ${it.hours} hours"
            }
        }
    }
}