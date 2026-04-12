package streetlight.server

import koala.CssFile
import koala.css.*
import koala.html.*
import streetlight.web.pages.AppBodyCss
import streetlight.web.pages.AppOverlayCss
import streetlight.web.ui.StarLightCss
import java.io.File

val SiteStyles by lazy {
    buildString {
        // debug at runtime like this:
        // println(BlockLabelCss)
        appendLine(StylesCss)

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
}

private val KtStyles = listOf(
    IconCss,
    LogoCss,
    ListingCss,
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
    MarkdownContentCss,

    AppBodyCss,
    AppOverlayCss,
)

private val Utilities = listOf(
    LayoutUtilityCss,
    DisplayUtilityCss,
    TextUtilityCss,
)