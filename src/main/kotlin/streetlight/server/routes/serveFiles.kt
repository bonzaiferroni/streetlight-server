package streetlight.server.routes

import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.Routing
import koala.JsFile
import streetlight.server.model.StreetlightRouting
import java.io.File

fun StreetlightRouting.serveFiles() {
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
    staticFiles("/gen/streetlight", File("../web/build/dist/wasmJs/productionExecutable")) {
//        cacheControl {
//            listOf(CacheControl.MaxAge(maxAgeSeconds = 600))
//        }
    }

    staticFiles("/dev/streetlight", File("../web/build/kotlin-webpack/js/developmentExecutable"))
    staticFiles("/js/streetlight", File("../web/build/kotlin-webpack/js/productionExecutable"))
}

const val GEN_PATH = "/gen/"