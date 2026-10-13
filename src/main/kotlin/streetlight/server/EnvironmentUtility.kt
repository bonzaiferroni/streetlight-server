package streetlight.server

import kabinet.utils.Environment
import java.io.File

enum class BuildMode { Development, Production }

object EnvKey {
    const val BUILD_ENV = "BUILD_ENV"
    const val DB_USER = "DB_USER"
    const val DB_URL = "DB_URL"
    const val DB_PASSWORD = "DB_PASSWORD"
    const val HOST_ADDRESS = "HOST_ADDRESS"
    const val S3_BUCKET = "S3_BUCKET"
    const val S3_REGION = "S3_REGION"
    const val S3_ENDPOINT = "S3_ENDPOINT"
    const val S3_ACCESS_KEY = "S3_ACCESS_KEY"
    const val S3_SECRET_KEY = "S3_SECRET_KEY"
    const val POSTMARK_SERVER_TOKEN = "POSTMARK_SERVER_TOKEN"
    const val POSTMARK_WEBHOOK_SECRET = "POSTMARK_WEBHOOK_SECRET"
    const val STREETLIGHT_SUPPORT_ADDRESS = "STREETLIGHT_SUPPORT_ADDRESS"
    const val STRIPE_SECRET_KEY = "STRIPE_SECRET_KEY"
    const val STRIPE_CONNECT_WEBHOOK_SECRET = "STRIPE_CONNECT_WEBHOOK_SECRET"
    const val GEMINI_KEY_A = "GEMINI_KEY_A"
    const val LM_PROVIDER = "LM_PROVIDER"
    const val LM_MODEL = "LM_MODEL"
    const val LM_CONTEXT_LENGTH = "LM_CONTEXT_LENGTH"
    const val LM_BASE_URL = "LM_BASE_URL"
}

/** The origin the site is reached at, as `https://streetlight.ing`, with no trailing slash. */
val Environment.hostAddress get() = read(EnvKey.HOST_ADDRESS).trimEnd('/')

/** The build mode named by `BUILD_ENV`; fails for any other value. */
val Environment.buildMode get() = when (val value = read(EnvKey.BUILD_ENV)) {
    "development" -> BuildMode.Development
    "production" -> BuildMode.Production
    else -> error("unknown ${EnvKey.BUILD_ENV}: $value")
}

/** The directory the web bundle is served from. In production it also holds the Koala bundle. */
val BuildMode.bundleDir get() = when (this) {
    BuildMode.Development -> "../web/build/kotlin-webpack/js/developmentExecutable/"
    BuildMode.Production -> "../bundle/"
}

/** The directory the Koala bundle is served from in development. */
const val DEV_KOALA_BUNDLE_DIR = "../koala/build/kotlin-webpack/js/productionExecutable/"

/**
 * The URL path of the bundle of this build. A production path changes with each build, so its files can be
 * cached for good.
 */
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