package streetlight.server.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole

fun Page.editor(label: String): Locator =
    getByRole(AriaRole.TEXTBOX, Page.GetByRoleOptions().setName(label))

fun Page.button(label: String): Locator =
    getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName(label))

fun Page.tab(label: String): Locator =
    getByText(label, Page.GetByTextOptions().setExact(true)).first()

fun Page.openTab(label: String): Locator = tab(label).also { it.click() }

fun Page.writeIn(label: String, text: String): Locator {
    val editor = editor(label)
    editor.click()
    editor.pressSequentially(text)
    assertThat(editor).containsText(text)
    return editor
}

fun Page.clickButton(label: String): Locator = button(label).also { it.click() }

fun Page.awaitMessage(text: String): Locator =
    getByText(text).also { assertThat(it).isVisible() }
