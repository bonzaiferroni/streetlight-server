package streetlight.server.html

import kotlinx.html.*
import streetlight.dto.EventInfo

fun HTML.eventsPage(events: List<EventInfo>) {
    head {
        title {
            +"Events | streetlight"
        }
    }
    body {
        h1 {
            +"Events"
        }
        events.forEach {
            p {
                +"${it.locationName}, ${it.areaName}"
                br
                +"${it.timeStart} - ${it.hours} hours"
            }
        }
    }
}