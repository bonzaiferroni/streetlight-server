package streetlight.server

import koala.JsBundle
import koala.PageResource
import koala.interop.buildHeadScript
import koala.modifier.*
import koala.interop.HeadScriptConfig
import koala.interop.RootSwitch
import koala.model.MarkerSheet
import streetlight.server.utils.printToFile
import streetlight.web.layout.CellGridCss
import streetlight.web.layout.EntityDialogCss
import streetlight.web.layout.FeedRowCss
import streetlight.web.layout.EntityBodyCss
import streetlight.web.layout.FeedSectionCss
import streetlight.web.page.AppBodyCss
import streetlight.web.component.LogoCss
import streetlight.web.page.DEFAULT_THEME_KEY
import streetlight.web.page.DefaultThemeOnly
import streetlight.web.page.PageThemeCss
import streetlight.web.page.AppOverlayCss
import streetlight.web.view.EarthCss
import streetlight.web.view.RouteDockCss
import streetlight.web.component.TagFilterMenuCss
import koala.jsFileOf
import koala.markdown.MarkdownCss
import koala.component.GlowControlCss
import koala.layout.LazyColumnCss
import koala.markdown.TextEditorCss
import streetlight.web.layout.FeedMode
import streetlight.web.layout.FeedRowStyle
import streetlight.web.component.StarToggleCss
import streetlight.web.layout.BodyCss
import streetlight.web.component.CuratorMenuStyle
import streetlight.web.view.InboxCss
import streetlight.web.view.ConnectCss
import streetlight.web.component.ConnectPickerCss
import streetlight.web.view.LayoutBuilderCss
import streetlight.web.layout.LayoutStyleCss
import streetlight.web.page.StreetlightCss
import streetlight.web.view.TalkLogCss
import streetlight.web.layout.TextDeltaCss
import koala.component.*
import koala.interop.*
import koala.layout.*
import koala.markdown.*

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
    SearchFieldCss,
    TableCss,
    ListItemCss,
    PopoverCss,
    SectionCss,
    ActionCss,
    TextLabelCss,
    MessageBoxCss,
    SwitchCss,
    OptionSwitchCss,
    SideTabsCss,
    SubrouterCss,
    FlowBlockCss,
    ItemsBlockCss,
    WireBlockCss,
    FillImageCss,
    CarouselCss,
    HeaderImageCss,
    FilePickerCss,
    BlockLabelCss,
    DialogCss,
    LottieCss,
    IconButtonCss,
    ContainImageCss,
    GridColumnsCss,
    RouteDockCss,
    TagFilterMenuCss,
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
    PageThemeCss,
    AppOverlayCss,
    EarthCss,
    TalkLogCss,
    FeedRowCss,
    EntityDialogCss,
    EntityBodyCss,
    FeedSectionCss,
    CellGridCss,
    StarToggleCss,
    MarkerSheet,
    TextDeltaCss,
    LayoutBuilderCss,
    LayoutStyleCss,
    InboxCss,
    CuratorMenuStyle,
    ConnectPickerCss,
    ConnectCss,
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

    // styles declared with components and layouts in koala
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
        RootSwitch(FeedRowStyle.Mode, FeedMode.Grid),
    ),
    rootModifiers = listOf(DefaultThemeOnly),
    rootStyleKey = DEFAULT_THEME_KEY,
)


