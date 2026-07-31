package streetlight.server.model

import kampfire.api.EmailAddress

class EmailRouter {
    val inboxes = mutableMapOf<EmailAddress, EmailInbox>()

    fun deliverEmail(email: Email) {
        val inbox = inbox(email.to)
        inbox.receive(email)
    }

    fun inbox(address: EmailAddress) = inboxes.getOrPut(address) { EmailInbox() }
}

class EmailInbox {
    val emails = mutableListOf<Email>()

    fun receive(email: Email) {
        emails.add(email)
    }

    fun latestOrNull() = emails.lastOrNull()
}