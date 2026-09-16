package streetlight.server

import kabinet.utils.Environment
import java.io.File

enum class BuildMode { Development, Production }

object EnvKey {
    const val BUILD_ENV = "BUILD_ENV"
}

val Environment.buildMode get() = when (val value = read(EnvKey.BUILD_ENV)) {
    "development" -> BuildMode.Development
    "production" -> BuildMode.Production
    else -> error("unknown ${EnvKey.BUILD_ENV}: $value")
}

val BuildMode.bundleDir get() = when (this) {
    BuildMode.Development -> "../web/build/kotlin-webpack/js/developmentExecutable/"
    BuildMode.Production -> "../bundle/"
}

val BuildMode.bundleBuildPath get() = "$BUNDLE_PATH_BASE${buildId}/"

val BuildMode.buildId get() = when (this) {
    BuildMode.Development -> "dev"
    BuildMode.Production -> File("../BUILD_ID").readText().trim()
}

val BuildMode.bundleCacheControl get() = when (this) {
    BuildMode.Development -> "no-cache"
    BuildMode.Production -> "public, max-age=31536000, immutable"
}

const val BUNDLE_PATH_BASE = "/js/streetlight/"