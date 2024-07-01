package streetlight.server.utilities

import kotlinx.html.FlowOrMetaDataOrPhrasingContent
import kotlinx.html.HTML
import kotlinx.html.Tag
import kotlinx.html.script
import kotlinx.html.unsafe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

inline fun <reified T> T.toJsVar(name: String) =
    "var $name = ${Json.encodeToJsonElement(this)};\n"

fun FlowOrMetaDataOrPhrasingContent.callFunction(name: String, vararg args: Any) {
    script {
        unsafe {
            raw("$name(${args.joinToString(", ")});")
        }
    }
}