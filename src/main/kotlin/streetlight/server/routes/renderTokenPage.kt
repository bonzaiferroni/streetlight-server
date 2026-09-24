package streetlight.server.routes

import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Token
import koala.PageResource
import koala.html.*
import streetlight.model.Api
import streetlight.model.data.AuthTokenType
import streetlight.server.db.datascope.redeemEmailVerification
import streetlight.server.model.ApiScope
import streetlight.web.pages.formSubmit
import streetlight.web.pages.messagePage
import streetlight.web.shells.passwordResetForm

/** The page of an emailed token link, by its type, or `null` without a token. */
suspend fun ApiScope.renderTokenPage(arg: String?, tokenType: AuthTokenType, resource: PageResource): HtmlRender? {
    val token = arg?.let { Token(it) } ?: return null

    return when (tokenType) {
        AuthTokenType.EmailVerification -> renderVerifyEmail(token, resource)
        AuthTokenType.PasswordReset -> renderPasswordResetForm(token, resource)
        AuthTokenType.AccountLockdown -> renderAccountLockdownConfirm(token, resource)
        AuthTokenType.AccountNotOwned -> renderAccountNotOwnedConfirm(token, resource)
    }
}

/** Verifies the email of [token] and shows the result. */
suspend fun ApiScope.renderVerifyEmail(token: Token, resource: PageResource): HtmlRender {
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
        messagePage(title, message, resource)
    }
}

/** Asks to confirm removing the address from the account; the confirmation posts [token]. */
fun renderAccountNotOwnedConfirm(token: Token, resource: PageResource): HtmlRender {
    return HtmlRender {
        messagePage(
            title = "Remove Address",
            message = """Confirming will remove this email address from the Streetlight account it was entered on. 
                   The account will no longer be able to send mail to you.""".trimIndent(),
            resource = resource,
        ) {
            formSubmit("Remove My Address", token, Api.AccountAction.AccountNotOwned)
        }
    }
}

/** Asks to confirm locking down the account; the confirmation posts [token]. */
fun renderAccountLockdownConfirm(token: Token, resource: PageResource): HtmlRender {
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
            resource = resource,
        ) {
            formSubmit("Lock My Account", token, Api.AccountAction.LockdownAccount)
        }
    }
}

fun renderPasswordResetForm(token: Token, resource: PageResource): HtmlRender {

    return HtmlRender {
        messagePage(
            title = "Password Reset",
            message = "Enter a new password",
            resource = resource,
        ) {
            passwordResetForm(token, resource)
        }
    }
}
