package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Token
import koala.css.*
import koala.html.*
import streetlight.model.Api
import streetlight.model.data.AuthTokenType
import streetlight.server.SiteStyles
import streetlight.server.db.services.redeemEmailVerification
import streetlight.server.model.ApiScope
import streetlight.web.pages.formSubmit
import streetlight.web.pages.messagePage
import streetlight.web.shells.passwordResetForm

suspend fun ApiScope.renderTokenPage(arg: String?, tokenType: AuthTokenType): HtmlRender? {
    val token = arg?.let { Token(it) } ?: return null

    return when (tokenType) {
        AuthTokenType.EmailVerification -> renderVerifyEmail(token)
        AuthTokenType.PasswordReset -> renderPasswordResetForm(token)
        AuthTokenType.AccountLockdown -> renderAccountLockdownConfirm(token)
        AuthTokenType.AccountNotOwned -> renderAccountNotOwnedConfirm(token)
    }
}

suspend fun ApiScope.renderVerifyEmail(token: Token): HtmlRender {
    val outcome = redeemEmailVerification(token)
    val title = when(outcome) {
        is Ok -> "Success"
        is Problem -> "Oops"
    }
    val message = when (outcome) {
        is Problem -> "There was a problem: ${outcome.message}"
        is Ok -> outcome.data
    }

    return HtmlRender {
        messagePage(title, message, SiteStyles)
    }
}

fun renderAccountNotOwnedConfirm(token: Token): HtmlRender {
    return HtmlRender {
        messagePage(
            title = "Remove Address",
            message = """Confirming will remove this email address from the Streetlight account it was entered on. 
                   The account will no longer be able to send mail to you.""".trimIndent(),
            styles = SiteStyles,
        ) {
            formSubmit("Remove My Address", token, Api.AccountAction.AccountNotOwned)
        }
    }
}

fun renderAccountLockdownConfirm(token: Token): HtmlRender {
    return HtmlRender {
        messagePage(
            title = "Lockdown Account",
            message = {
                column {
                    textBlock("""
                        Confirming will sign out all existing sessions for your account and 
                        disable sign in until you set a new password. 
                        A link will be emailed to you that will allow you to set a new password.
                """.trimIndent())
                    textBlock("""
                        This should only be necessary if you believe someone has unauthorized access to your account. 
                    """.trimIndent())
                }
            },
            styles = SiteStyles,
        ) {
            formSubmit("Remove My Address", token, Api.AccountAction.AccountNotOwned)
        }
    }
}

fun renderPasswordResetForm(token: Token): HtmlRender {

    return HtmlRender {
        messagePage(
            title = "Password Reset",
            message = "Enter a new password",
            styles = SiteStyles,
        ) {
            passwordResetForm(token)
        }
    }
}
