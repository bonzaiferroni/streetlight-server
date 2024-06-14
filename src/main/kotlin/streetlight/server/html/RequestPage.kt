package streetlight.server.html

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.onClick
import kotlinx.html.p
import kotlinx.html.script
import streetlight.dto.EventInfo
import streetlight.model.Performance

fun HTML.requestPage(performances: List<Performance>) {
    pageHeader("Request")
    body {
        h1 {
            +"Request"
        }
        performances.forEach {
            button {
                id = "button-${it.id}"
                classes = setOf("btn", "btn-primary")
                onClick = "makeRequest(${it.id})"
                +it.name
            }
        }
        script {
            src = "static/request.js"
        }
    }

}