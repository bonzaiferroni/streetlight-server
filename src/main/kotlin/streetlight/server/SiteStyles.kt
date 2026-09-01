package streetlight.server

import koala.CssFile
import koala.css.*
import koala.html.*
import koala.model.AltitudeCss
import koala.model.MarkerSheet
import streetlight.server.utils.printToFile
import streetlight.web.layouts.CellContentCss
import streetlight.web.layouts.FeedPostCss
import streetlight.web.layouts.FeedProtoCss
import streetlight.web.pages.AppBodyCss
import streetlight.web.pages.AppOverlayCss
import streetlight.web.ui.EarthCss
import koala.html.NavMenuCss
import koala.markdown.MarkdownCss
import koala.model.GlowControlCss
import koala.model.LazyColumnCss
import koala.model.TextEditorCss
import streetlight.web.layouts.LightControlCss
import streetlight.web.ui.BodyCss
import streetlight.web.ui.InboxCss
import streetlight.web.ui.LayoutBuilderCss
import streetlight.web.ui.LayoutStyleCss
import streetlight.web.ui.StarLightCss
import streetlight.web.ui.StreetlightCss
import streetlight.web.ui.TalkLogCss
import streetlight.web.ui.TextDeltaCss
import java.io.File

private val KtStyles = listOf(
    // koala module
    IconCss,
    LogoCss,
    MarkdownCss,
    ListingCss,
    TableCss,
    ListItemCss,
    PopoverCss,
    SwapBlockCss,
    SectionCss,
    ActionCss,
    TextLabelCss,
    MessageBoxCss,
    SwitchCss,
    FlowBlockCss,
    ItemsBlockCss,
    ShellBoxCss,
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
    StarLightCss,
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

    // web module
    StreetlightCss,
    AppBodyCss,
    AppOverlayCss,
    EarthCss,
    TalkLogCss,
    FeedPostCss,
    FeedProtoCss,
    CellContentCss,
    LightControlCss,
    MarkerSheet,
    AltitudeCss,
    TextDeltaCss,
    LayoutBuilderCss,
    LayoutStyleCss,
    InboxCss,
)

private val Utilities = listOf(
    LayoutUtilityCss,
    DisplayUtilityCss,
    TextUtilityCss,
)

val SiteStyles = buildString {
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
}.also { printToFile(it, "../debug/site-styles.css") }