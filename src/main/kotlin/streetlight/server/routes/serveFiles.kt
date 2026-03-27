package streetlight.server.routes

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import koala.css.CssManifest

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

//    get(CssFiles.genElements.path) {
//        call.respondText(
//            text = CssManifest.joinToString("\n"),
//            contentType = ContentType.Text.CSS
//        )
//    }
}