package streetlight.server.routes

import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import koala.DEV_PATH
import koala.PROD_PATH
import streetlight.server.model.ApiScope
import java.io.File

fun ApiScope.serveFiles() {
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
    staticFiles(PROD_PATH, File("../web/build/kotlin-webpack/js/productionExecutable")) {
//        cacheControl {
//            listOf(CacheControl.MaxAge(maxAgeSeconds = 600))
//        }
    }

    // staticFiles(DEV_PATH, File("../web/build/kotlin-webpack/js/developmentExecutable"))
    val webBundle = File("../web/build/kotlin-webpack/js/developmentExecutable/web.js").readBytes()

    get("${DEV_PATH}web.js") {
        call.respondBytes(webBundle, ContentType.Application.JavaScript)
    }
}

