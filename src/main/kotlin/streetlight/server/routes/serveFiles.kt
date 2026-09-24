package streetlight.server.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.defaultForFilePath
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.header
import io.ktor.server.routing.Route
import kabinet.utils.Environment
import klutch.server.provide
import streetlight.server.BuildMode
import streetlight.server.buildMode
import streetlight.server.bundleCacheControl
import streetlight.server.bundleDir
import streetlight.server.bundleBuildPath
import streetlight.server.model.ApiScope
import java.io.File

fun ApiScope.serveFiles() {
    val env = provide<Environment>()
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

    serveBundle(env.buildMode)

//    staticFiles(PROD_PATH, File("../web/build/kotlin-webpack/js/productionExecutable")) {
//        cacheControl {
//            listOf(CacheControl.MaxAge(maxAgeSeconds = 600))
//        }
//    }

//    val webBundle = File("../web/build/kotlin-webpack/js/developmentExecutable/web.js").readBytes()
//
//    get("${DEV_PATH}web.js") {
//        call.respondBytes(webBundle, ContentType.Application.JavaScript)
//    }
}

/** Serves the web bundle of [buildMode] with its cache policy. */
fun Route.serveBundle(buildMode: BuildMode) {
    staticFiles(buildMode.bundleBuildPath, File(buildMode.bundleDir)) {
        modify { _, call ->
            call.response.header(HttpHeaders.CacheControl, buildMode.bundleCacheControl)
        }
    }
}