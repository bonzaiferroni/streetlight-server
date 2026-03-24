package streetlight.server.routes

import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import koala.CssFile
import koala.CssFiles
import koala.genPath
import koala.html.ICON_STYLES
import koala.html.LOGO_STYLES
import koala.html.POPOVER_STYLES

fun Routing.serveFiles() {
    uploadFolder.mkdirs()
    wwwFolder.mkdirs()

    staticFiles("/upload", uploadFolder)

    staticFiles("/www", wwwFolder) {
        contentType { file ->
            if (file.extension == "map") ContentType.Application.Json
            else ContentType.defaultForFilePath(file.path)
        }
//        cacheControl {
//            listOf(CacheControl.MaxAge(maxAgeSeconds = 600))
//        }
    }

    get(CssFiles.genElements, ICON_STYLES, LOGO_STYLES, POPOVER_STYLES)
}

fun Routing.get(file: CssFile, vararg styles: String) {
    get(file.path) {
        call.respondText(
            text = styles.joinToString("\n\n"),
            contentType = ContentType.Text.CSS
        )
    }
}