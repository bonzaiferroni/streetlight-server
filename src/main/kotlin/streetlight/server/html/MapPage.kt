package streetlight.server.html

import kotlinx.html.*
import kotlinx.serialization.Serializable
import streetlight.model.Location
import streetlight.server.models.GeoPoint
import streetlight.server.utilities.toJsVar

fun HTML.mapPage(locations: List<Location>) {
    pageHeader("Map") {
        link {
            rel = "stylesheet"
            href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
        }
        script {
            src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
        }
    }
    body {
        h1 {
            +"map"
        }
        div {
            id = "map"
            style = "height: 400px"
        }
        script(type = ScriptType.textJavaScript) {
            unsafe {
                raw(locations
                    .map { GeoPoint(it.latitude, it.longitude).toList() }
                    .toJsVar("markers")
                )
            }
        }
        script {
            src = "static/map.js"
        }
    }
}