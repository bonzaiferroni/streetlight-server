package streetlight.server.api

import kampfire.api.Username
import kampfire.api.toMarkdown
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.CommentId
import streetlight.model.data.NewComment
import streetlight.model.data.SpaceType
import streetlight.model.data.UpdatedComment
import streetlight.server.db.tables.CommentTable
import streetlight.server.seedGalaxy
import streetlight.server.toDataOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class TalkApiTest : ApiTest() {

    @Test
    fun `a signed-in user's comment is stored under their username`() = runApiTest {
        val starId = registerUser("alice")
        val galaxyId = server.seedGalaxy(starId)
        signInAs("alice")

        postApi(Api.Talk.CreateComment, NewComment(galaxyId.value, SpaceType.Galaxy, null, TEXT.toMarkdown())).toDataOrThrow()

        val comments = getApi(Api.Talk.ReadGalaxy, galaxyId).toDataOrThrow()
        assertEquals(1, comments.size, "the galaxy should hold the comment")
        assertEquals(Username("alice"), comments.single().username, "the comment should name its author")
        assertEquals(TEXT, comments.single().text.value)
    }

    @Test
    fun `a comment appears in the history of its space`() = runApiTest {
        val starId = registerUser("alice")
        val galaxyId = server.seedGalaxy(starId)
        signInAs("alice")
        postApi(Api.Talk.CreateComment, NewComment(galaxyId.value, SpaceType.Galaxy, null, TEXT.toMarkdown())).toDataOrThrow()

        val history = getApi(Api.Talk.ReadHistory) {
            writeParam(it.spaceId, galaxyId.value)
            writeParam(it.spaceType, SpaceType.Galaxy)
        }.toDataOrThrow()

        assertEquals(listOf(TEXT), history.map { it.text.value }, "the history should hold the comment")
    }

    @Test
    fun `only the author of a comment can update it`() = runApiTest {
        val starId = registerUser("alice")
        val galaxyId = server.seedGalaxy(starId)
        signInAs("alice")
        val commentId = postApi(Api.Talk.CreateComment, NewComment(galaxyId.value, SpaceType.Galaxy, null, TEXT.toMarkdown())).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val updatedByOther = postApi(Api.Talk.UpdateComment, UpdatedComment(commentId, galaxyId.value, "Bob's edit".toMarkdown())).toDataOrThrow()
        assertEquals(false, updatedByOther, "another user should not edit the comment")
        assertEquals(TEXT, textOf(commentId), "the comment should keep its text")

        signInAs("alice")
        val updatedByAuthor = postApi(Api.Talk.UpdateComment, UpdatedComment(commentId, galaxyId.value, "Alice's edit".toMarkdown())).toDataOrThrow()
        assertEquals(true, updatedByAuthor, "the author should edit the comment")
        assertEquals("Alice's edit", textOf(commentId), "the comment should hold the new text")
    }

    private fun textOf(commentId: CommentId) = transaction {
        CommentTable.selectAll().where { CommentTable.id.eq(commentId.value) }.single()[CommentTable.text].value
    }
}

private const val TEXT = "The fox den is open tonight."
