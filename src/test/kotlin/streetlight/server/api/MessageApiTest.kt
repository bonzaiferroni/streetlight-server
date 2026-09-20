package streetlight.server.api

import kampfire.api.Username
import kampfire.api.toMarkdown
import streetlight.model.Api
import streetlight.model.data.ChatId
import streetlight.model.data.ChatMessageRequest
import streetlight.model.data.ChatRequest
import streetlight.model.data.NewMessage
import streetlight.model.data.ReplyMessage
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageApiTest : ApiTest() {

    @Test
    fun `a user sends a message and the recipient finds the chat in their inbox`() = runApiTest {
        registerUser("alice")
        registerUser("bob")

        signInAs("alice")
        postApi(Api.Messages.SendNew, NewMessage(Username("bob"), "Hello", "Hi Bob".toMarkdown())).toDataOrThrow()
        val aliceChats = getApi(Api.Messages.Inbox).toDataOrThrow().chats

        signInAs("bob")
        val bobChats = getApi(Api.Messages.Inbox).toDataOrThrow().chats

        assertEquals(1, aliceChats.size, "the sender should see the chat")
        assertEquals(1, bobChats.size, "the recipient should see the chat")
        assertEquals(aliceChats.single().chatId, bobChats.single().chatId, "both should see the same chat")
    }

    @Test
    fun `a message to an unknown username is refused`() = runApiTest {
        registerUser("alice")
        signInAs("alice")

        postApi(Api.Messages.SendNew, NewMessage(Username("nobody"), null, "Hello?".toMarkdown())).toProblemOrThrow()

        assertTrue(getApi(Api.Messages.Inbox).toDataOrThrow().chats.isEmpty(), "no chat should be created")
    }

    @Test
    fun `only the members of a chat can read its messages`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        registerUser("carol")
        val chatId = startChat(from = "alice", to = "bob")

        signInAs("carol")
        postApi(Api.Messages.ReadChatMessages, ChatMessageRequest(chatId)).toProblemOrThrow()

        signInAs("bob")
        val messages = postApi(Api.Messages.ReadChatMessages, ChatMessageRequest(chatId)).toDataOrThrow()
        assertEquals(1, messages.size, "a member should read the chat")
        assertEquals(Username("alice"), messages.single().author)
    }

    @Test
    fun `only the members of a chat can reply to it`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        registerUser("carol")
        val chatId = startChat(from = "alice", to = "bob")

        signInAs("carol")
        postApi(Api.Messages.SendReply, ReplyMessage(chatId, "Let me in".toMarkdown())).toProblemOrThrow()

        signInAs("bob")
        val messages = postApi(Api.Messages.ReadChatMessages, ChatMessageRequest(chatId)).toDataOrThrow()
        assertEquals(1, messages.size, "a non-member's reply should not be stored")
    }

    @Test
    fun `a reply is read by the other member`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        val chatId = startChat(from = "alice", to = "bob")

        signInAs("bob")
        postApi(Api.Messages.SendReply, ReplyMessage(chatId, "Hi Alice".toMarkdown())).toDataOrThrow()

        signInAs("alice")
        val messages = postApi(Api.Messages.ReadChatMessages, ChatMessageRequest(chatId)).toDataOrThrow()
        assertEquals(2, messages.size, "the chat should hold both messages")
        assertEquals(Username("bob"), messages.first().author, "the newest message should come first")
    }

    @Test
    fun `archiving a chat moves it out of the inbox for that user only`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        val chatId = startChat(from = "alice", to = "bob")

        postApi(Api.Messages.ArchiveChat, chatId).toDataOrThrow()

        assertTrue(getApi(Api.Messages.Inbox).toDataOrThrow().chats.isEmpty(), "the archived chat should leave the inbox")
        val archive = postApi(Api.Messages.ReadChats, ChatRequest(isArchive = true)).toDataOrThrow()
        assertEquals(chatId, archive.single().chatId, "the chat should be in the archive")

        signInAs("bob")
        assertEquals(1, getApi(Api.Messages.Inbox).toDataOrThrow().chats.size, "the other member's inbox should be unchanged")
    }

    @Test
    fun `a user who is not in a chat cannot archive it`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        registerUser("carol")
        startChat(from = "alice", to = "bob")
        val chatId = getApi(Api.Messages.Inbox).toDataOrThrow().chats.single().chatId

        signInAs("carol")
        postApi(Api.Messages.ArchiveChat, chatId).toProblemOrThrow()

        signInAs("alice")
        assertEquals(1, getApi(Api.Messages.Inbox).toDataOrThrow().chats.size, "the chat should stay in the inbox")
    }

    private suspend fun ApiTestScope.startChat(from: String, to: String): ChatId {
        signInAs(from)
        postApi(Api.Messages.SendNew, NewMessage(Username(to), "Hello", "Hi $to".toMarkdown())).toDataOrThrow()
        return getApi(Api.Messages.Inbox).toDataOrThrow().chats.single().chatId
    }
}
