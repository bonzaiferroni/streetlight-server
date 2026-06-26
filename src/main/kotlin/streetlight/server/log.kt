package streetlight.server

import kabinet.console.LogLevel
import kabinet.console.globalConsole
import kotlin.reflect.KFunction

fun KFunction<*>.log(message: String, level: LogLevel = LogLevel.Info) {
    globalConsole.log(name, level, message)
}