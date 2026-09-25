package streetlight.server

import koala.JsBundle
import koala.PageResource
import koala.interop.buildHeadScript
import koala.modifier.*
import koala.html.*
import koala.interop.HeadScriptConfig
import koala.interop.RootSwitch
import koala.model.AltitudeCss
import koala.model.MarkerSheet
import streetlight.server.utils.printToFile
import streetlight.web.layouts.CellGridCss
import streetlight.web.layouts.FeedProtoCss
import streetlight.web.layouts.FeedSectionCss
import streetlight.web.pages.AppBodyCss
import streetlight.web.pages.AppOverlayCss
import streetlight.web.ui.EarthCss
import streetlight.web.ui.RouteDockCss
import koala.jsFileOf
import koala.markdown.MarkdownCss
import koala.model.GlowControlCss
import koala.model.LazyColumnCss
import koala.model.TextEditorCss
import streetlight.web.layouts.FeedMode
import streetlight.web.layouts.FeedRow
import streetlight.web.ui.StarToggleCss
import streetlight.web.ui.BodyCss
import streetlight.web.ui.CuratorMenuStyle
import streetlight.web.ui.InboxCss
import streetlight.web.ui.LayoutBuilderCss
import streetlight.web.ui.LayoutStyleCss
import streetlight.web.ui.StreetlightCss
import streetlight.web.ui.TalkLogCss
import streetlight.web.ui.TextDeltaCss

/**
 * The resources of every server-rendered page: the script bundle, the stylesheet built from the Kotlin styles,
 * and the head script.
 *
 * In development the stylesheet is also written to `debug/site-styles.css`.
 */
class ServerResource(mode: BuildMode): PageResource {

    override val bundle = ServerBundle(mode)
    override val styles = BuildStyles()
    override val headScript = buildHeadScript(headScriptConfig)

    init {
        if (mode == BuildMode.Development) {
            printToFile(styles, "../debug/site-styles.css")
        }
    }
}

private val KtStyles = listOf(
    // koala module
    IconCss,
    LogoCss,
    MarkdownCss,
    ListingCss,
    TableCss,
    ListItemCss,
    PopoverCss,
    SectionCss,
    ActionCss,
    TextLabelCss,
    MessageBoxCss,
    SwitchCss,
    FlowBlockCss,
    ItemsBlockCss,
    WireBlockCss,
    FillImageCss,
    CarouselCss,
    HeaderImageCss,
    FilePickerCss,
    BlockLabelCss,
    ImageChooserCss,
    DialogCss,
    LottieCss,
    IconButtonCss,
    ContainImageCss,
    GridColumnsCss,
    SwapCss,
    RouteDockCss,
    BodyCss,
    HrCss,
    GlowControlCss,
    TextEditorCss,
    LazyColumnCss,
    ProgressBarCss,
    TabsCss,

    // web module
    StreetlightCss,
    AppBodyCss,
    AppOverlayCss,
    EarthCss,
    TalkLogCss,
    FeedProtoCss,
    FeedSectionCss,
    CellGridCss,
    StarToggleCss,
    MarkerSheet,
    AltitudeCss,
    TextDeltaCss,
    LayoutBuilderCss,
    LayoutStyleCss,
    InboxCss,
    CuratorMenuStyle,
)

private fun BuildStyles() = buildString {
    appendLine(ResetCss)
    appendLine(StylesCss)
    appendLine(TypographyCss)
    appendLine(LayoutCss)
    appendLine(ThemeCss)
    appendLine(MagicCss)
    appendLine(ButtonCss)
    appendLine(GeoMapCss)

    // styles declared with elements in koala.html
    KtStyles.forEach {
        appendLine(it)
    }

    appendLine(DisplayCss)
    appendLine(TextCss)
}

class ServerBundle(mode: BuildMode): JsBundle {
    override val web = jsFileOf("web.js", basePath = mode.bundleBuildPath)
    override val passwordReset = jsFileOf("passwordReset.js", basePath = mode.bundleBuildPath)
    override val koala = jsFileOf("koala.js", basePath = mode.bundleBuildPath)
}

private val headScriptConfig = HeadScriptConfig(
    rootSwitches = listOf(
        RootSwitch(FeedRow.Mode, FeedMode.Grid),
    ),
)


