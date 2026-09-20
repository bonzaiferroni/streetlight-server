package streetlight.server.api

import kampfire.api.EmailAddress
import kampfire.api.Slug
import kampfire.api.Username
import kampfire.api.toMarkdown
import kampfire.api.toSlug
import kampfire.model.CoreProblem
import kampfire.model.HttpProblem
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.HostType
import streetlight.model.data.PostEdit
import streetlight.model.data.PostId
import streetlight.model.data.PostType
import streetlight.model.data.StarId
import streetlight.server.db.tables.GalaxyHostTable
import streetlight.server.db.tables.GalaxyTable
import streetlight.server.db.tables.PostTable
import streetlight.server.DENVER_AREA
import streetlight.server.registerAdmin
import streetlight.server.seedLocation
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GalaxyApiTest : ApiTest() {

    @Test
    fun `a guest cannot create a galaxy`() = runApiTest {
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toProblemOrThrow()

        assertEquals(0, galaxyCount(), "a guest should leave no galaxy behind")
    }

    @Test
    fun `a user creates a galaxy and becomes its creator`() = runApiTest {
        val starId = registerUser("alice")
        signInAs("alice")

        val slug = postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()

        assertEquals(SLUG.toSlug(), slug)
        assertEquals(1, galaxyCount(), "one create should leave one galaxy")
        assertEquals(HostType.Creator, hostTypeOf(galaxyIdOf(SLUG), starId), "the author should host the galaxy")
    }

    @Test
    fun `a galaxy cannot take a slug already in use`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()

        registerUser("bob")
        signInAs("bob")
        val problem = postApi(Api.Galaxies.CreateGalaxy, galaxyEdit(name = "Another Galaxy")).toProblemOrThrow()

        assertEquals(HttpProblem.Conflict, problem)
        assertEquals(1, galaxyCount(), "the second galaxy should not be stored")
    }

    @Test
    fun `a galaxy with an invalid slug is refused`() = runApiTest {
        registerUser("alice")
        signInAs("alice")

        val problem = postApi(Api.Galaxies.CreateGalaxy, galaxyEdit().copy(slug = Slug("Fox Friends!"))).toProblemOrThrow()

        assertEquals(CoreProblem.InvalidSlug, problem)
        assertEquals(0, galaxyCount(), "an invalid slug should not be stored")
    }

    @Test
    fun `a user who does not host a galaxy cannot update it`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()
        val galaxyId = galaxyIdOf(SLUG)

        registerUser("bob")
        signInAs("bob")
        postApi(Api.Galaxies.UpdateGalaxy, galaxyEdit(name = "Bob's Galaxy").copy(galaxyId = galaxyId)).toProblemOrThrow()
        assertEquals(NAME, galaxyNameOf(galaxyId), "another user should not rename the galaxy")

        signInAs("alice")
        postApi(Api.Galaxies.UpdateGalaxy, galaxyEdit(name = "Renamed Galaxy").copy(galaxyId = galaxyId)).toDataOrThrow()
        assertEquals("Renamed Galaxy", galaxyNameOf(galaxyId), "the creator should rename the galaxy")
    }

    @Test
    fun `updating a galaxy leaves its slug unchanged`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()
        val galaxyId = galaxyIdOf(SLUG)

        val slug = postApi(
            Api.Galaxies.UpdateGalaxy,
            galaxyEdit(name = "Renamed Galaxy").copy(galaxyId = galaxyId, slug = Slug("fox-club")),
        ).toDataOrThrow()

        assertEquals(SLUG.toSlug(), slug, "the update should report the original slug")
        assertEquals(SLUG, galaxySlugOf(galaxyId), "the slug is written at founding only")
        assertEquals("Renamed Galaxy", galaxyNameOf(galaxyId), "the rest of the edit should apply")
    }

    @Test
    fun `an admin can update a galaxy they do not host`() = runApiTest {
        registerUser("alice")
        signInAs("alice")
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()
        val galaxyId = galaxyIdOf(SLUG)

        server.registerAdmin(Username("root"), EmailAddress("root@gmail.com"))
        signInAs("root")
        postApi(Api.Galaxies.UpdateGalaxy, galaxyEdit(name = "Admin Edit").copy(galaxyId = galaxyId)).toDataOrThrow()

        assertEquals("Admin Edit", galaxyNameOf(galaxyId), "an admin should rename any galaxy")
    }

    @Test
    fun `only the author of a post can update it`() = runApiTest {
        val postId = alicePostsInHerGalaxy()

        registerUser("bob")
        signInAs("bob")
        postApi(Api.Galaxies.UpdatePost, postEdit(postId, "Bob's edit")).toProblemOrThrow()
        assertEquals(POST_TEXT, postTextOf(postId), "another user should not edit the post")

        signInAs("alice")
        postApi(Api.Galaxies.UpdatePost, postEdit(postId, "Alice's edit")).toDataOrThrow()
        assertEquals("Alice's edit", postTextOf(postId), "the author should edit the post")
    }

    @Test
    fun `only the author of a post can remove it`() = runApiTest {
        val postId = alicePostsInHerGalaxy()

        registerUser("bob")
        signInAs("bob")
        val removedByOther = postApi(Api.Galaxies.RemovePost, postId).toDataOrThrow()
        assertEquals(false, removedByOther, "another user should not remove the post")
        assertNotNull(postTextOf(postId), "the post should remain")

        signInAs("alice")
        val removedByAuthor = postApi(Api.Galaxies.RemovePost, postId).toDataOrThrow()
        assertEquals(true, removedByAuthor, "the author should remove the post")
        assertNull(postTextOf(postId), "the post should be gone")
    }

    private suspend fun ApiTestScope.alicePostsInHerGalaxy(): PostId {
        val starId = registerUser("alice")
        signInAs("alice")
        postApi(Api.Galaxies.CreateGalaxy, galaxyEdit()).toDataOrThrow()
        val location = server.seedLocation(starId)
        val edit = PostEdit(
            postId = null,
            galaxyId = galaxyIdOf(SLUG),
            postType = PostType.Location,
            recordId = location.locationId.value,
            title = "A first post",
            text = POST_TEXT.toMarkdown(),
        )
        return postApi(Api.Galaxies.CreatePost, edit).toDataOrThrow().postId
    }

    private fun galaxyEdit(name: String = NAME) = GalaxyEdit(name = name, slug = Slug(SLUG), geoRect = DENVER_AREA, marks = emptyList())

    private fun postEdit(postId: PostId, text: String) = PostEdit(
        postId = postId,
        galaxyId = galaxyIdOf(SLUG),
        postType = PostType.Location,
        recordId = transaction { PostTable.selectAll().where { PostTable.id.eq(postId) }.single()[PostTable.locationId]!!.value },
        title = "A first post",
        text = text.toMarkdown(),
    )

    private fun galaxyCount() = transaction { GalaxyTable.selectAll().count().toInt() }

    private fun galaxyIdOf(slug: String) = transaction {
        GalaxyId(GalaxyTable.selectAll().where { GalaxyTable.slug.eq(slug) }.single()[GalaxyTable.id].value)
    }

    private fun galaxySlugOf(galaxyId: GalaxyId) = transaction {
        GalaxyTable.selectAll().where { GalaxyTable.id.eq(galaxyId) }.single()[GalaxyTable.slug]
    }

    private fun galaxyNameOf(galaxyId: GalaxyId) = transaction {
        GalaxyTable.selectAll().where { GalaxyTable.id.eq(galaxyId) }.single()[GalaxyTable.name]
    }

    private fun hostTypeOf(galaxyId: GalaxyId, starId: StarId) = transaction {
        GalaxyHostTable.selectAll()
            .where { GalaxyHostTable.galaxyId.eq(galaxyId.value) and GalaxyHostTable.hostId.eq(starId.value) }
            .singleOrNull()?.get(GalaxyHostTable.hostType)
    }

    private fun postTextOf(postId: PostId) = transaction {
        PostTable.selectAll().where { PostTable.id.eq(postId) }.singleOrNull()?.get(PostTable.text)?.value
    }
}

private const val NAME = "Fox Friends"
private const val SLUG = "fox-friends"
private const val POST_TEXT = "The fox den is open tonight."
