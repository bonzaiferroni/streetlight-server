package streetlight.server

import koala.CssFile
import koala.JsBundle
import koala.PageResource
import koala.modifier.*
import koala.html.*
import koala.model.AltitudeCss
import koala.model.MarkerSheet
import streetlight.server.utils.printToFile
import streetlight.web.layouts.CellContentCss
import streetlight.web.layouts.FeedProtoCss
import streetlight.web.pages.AppBodyCss
import streetlight.web.pages.AppOverlayCss
import streetlight.web.ui.EarthCss
import koala.html.NavMenuCss
import koala.jsFileOf
import koala.markdown.MarkdownCss
import koala.model.GlowControlCss
import koala.model.LazyColumnCss
import koala.model.TextEditorCss
import streetlight.web.ui.StarToggleCss
import streetlight.web.ui.BodyCss
import streetlight.web.ui.CuratorMenuStyle
import streetlight.web.ui.InboxCss
import streetlight.web.ui.LayoutBuilderCss
import streetlight.web.ui.LayoutStyleCss
import streetlight.web.ui.StreetlightCss
import streetlight.web.ui.TalkLogCss
import streetlight.web.ui.TextDeltaCss
import java.io.File

class ServerResource(mode: BuildMode): PageResource {

    override val bundle = ServerBundle(mode)
    override val styles = BuildStyles()

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
    FeatureImageCss,
    GridColumnsCss,
    SwapCss,
    TextUtilitySheet,
    NavMenuCss,
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
    CellContentCss,
    StarToggleCss,
    MarkerSheet,
    AltitudeCss,
    TextDeltaCss,
    LayoutBuilderCss,
    LayoutStyleCss,
    InboxCss,
    CuratorMenuStyle,
)

private val Utilities = listOf(
    LayoutUtilityCss,
    DisplayUtilityCss,
    TextUtilityCss,
)

private fun BuildStyles() = buildString {
    // debug at runtime like this:
    // println(TextUtilityCss.toStylesheet())
    appendLine(StylesCss)
    appendLine(TypographyCss)
    appendLine(LayoutCss)
    appendLine(ThemeCss)
    appendLine(MagicCss)
    appendLine(ButtonCss)


    // css files located in /www/css
    CssFile.forEach {
        appendLine(File("../${it.url}").readText())
    }

    // styles declared with elements in koala.html
    KtStyles.forEach {
        appendLine(it)
    }

    // utilities declared in koala.css
    Utilities.forEach {
        appendLine(it.toStylesheet())
    }
}

class ServerBundle(mode: BuildMode): JsBundle {
    override val web = jsFileOf("web.js", basePath = mode.bundleBuildPath)
    override val passwordReset = jsFileOf("passwordReset.js", basePath = mode.bundleBuildPath)
}


