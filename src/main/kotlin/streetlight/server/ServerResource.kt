package streetlight.server

import koala.JsBundle
import koala.PageResource
import koala.interop.buildHeadScript
import koala.interop.HeadScriptConfig
import koala.interop.RootSwitch
import streetlight.server.utils.printToFile
import streetlight.web.page.DEFAULT_THEME_KEY
import streetlight.web.page.DefaultThemeOnly
import koala.jsFileOf
import streetlight.web.layout.FeedMode
import streetlight.web.layout.FeedRowStyle
import koala.interop.*
import streetlight.web.page.buildSiteStyles

/**
 * The resources of every server-rendered page: the script bundle, the stylesheet built from the Kotlin styles,
 * and the head script.
 *
 * In development the stylesheet is also written to `debug/site-styles.css`.
 */
class ServerResource(mode: BuildMode): PageResource {

    override val bundle = ServerBundle(mode)
    override val styles = buildSiteStyles()
    override val headScript = buildHeadScript(headScriptConfig)

    init {
        if (mode == BuildMode.Development) {
            printToFile(styles, "../debug/site-styles.css")
        }
    }
}

class ServerBundle(mode: BuildMode): JsBundle {
    override val web = jsFileOf("web.js", basePath = mode.bundleBuildPath)
    override val passwordReset = jsFileOf("passwordReset.js", basePath = mode.bundleBuildPath)
    override val locationApp = jsFileOf("locationApp.js", basePath = mode.bundleBuildPath)
    override val koala = jsFileOf("koala.js", basePath = mode.bundleBuildPath)
}

private val headScriptConfig = HeadScriptConfig(
    rootSwitches = listOf(
        RootSwitch(FeedRowStyle.Mode, FeedMode.Grid),
    ),
    rootModifiers = listOf(DefaultThemeOnly),
    rootStyleKey = DEFAULT_THEME_KEY,
)
