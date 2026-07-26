package streetlight.server.routes

import kampfire.api.ActionResult
import kampfire.api.PostEndpoint
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Token
import koala.css.*
import koala.html.*
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.form
import kotlinx.html.hiddenInput
import kotlinx.html.submitInput
import streetlight.model.Api
import streetlight.server.SiteStyles
import streetlight.server.db.services.redeemEmailVerification
import streetlight.server.model.ApiScope
import streetlight.web.pages.staticPage
import streetlight.web.ui.BodyStyle

fun renderActionReport(arg: String?): HtmlRender {
    val result = ActionResult.of(arg)
    val title = when (result) {
        ActionResult.Success -> "Success"
        else -> "Oops"
    }
    val message = when(result) {
        ActionResult.Invalid -> "The action was not successful."
        ActionResult.InternalError -> "Something went wrong on our end."
        ActionResult.Success -> "Success! You may close this tab."
    }
    return tokenPage(title, message)
}

suspend fun ApiScope.renderVerifyEmail(arg: String?): HtmlRender? {
    val token = arg?.let { Token(it) } ?: return null
    val outcome = redeemEmailVerification(token)
    val title = when(outcome) {
        is Ok -> "Success"
        is Problem -> "Oops"
    }
    val message = when (outcome) {
        is Problem -> "There was a problem: ${outcome.message}"
        is Ok -> outcome.data
    }

    return tokenPage(title, message)
}

fun renderAccountNotOwned(arg: String?): HtmlRender? {
    val token = arg?.let { Token(it) } ?: return null

    return tokenPage(
        title = "Remove Address",
        message = """Confirming will remove this email address from the Streetlight account it was entered on. 
                   The account will no longer be able to send mail to you.""".trimIndent(),
    ) {
        formSubmit("Remove My Address", token, Api.Tokens.AccountNotOwned)
    }
}

fun tokenPage(
    title: String,
    message: String,
    block: FlowContent.() -> Unit = { }
): HtmlRender {
    return HtmlRender {
        staticPage("$title | Streetlight", SiteStyles) {
            column(BodyStyle.Column) {
                filigree { heading1(title) }
                card(modify(MaxWidth64, AlignSelfCenter)) {
                    textBlock(message, modify(Padding1))
                    block()
                }
            }
        }
    }
}

fun FlowContent.formSubmit(
    text: String,
    token: Token,
    endpoint: PostEndpoint<*, *>
) = form(action = endpoint.path, method = FormMethod.post) {
    addModifiers(AlignSelfEnd)
    hiddenInput(name = "token") { value = token.value }
    submitInput {
        addModifiers(BtnKey.Class)
        value = text
    }
}