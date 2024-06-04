package streetlight.server.utilities

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

inline fun <reified T> T.toJsVar(name: String) =
    "var $name = ${Json.encodeToJsonElement(this)};\n"