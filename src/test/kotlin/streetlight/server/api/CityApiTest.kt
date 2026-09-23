package streetlight.server.api

import streetlight.model.Api
import streetlight.model.data.CityEdit
import streetlight.model.data.CityId
import streetlight.server.loginStar
import streetlight.server.registerStar
import streetlight.server.seedCity
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class CityApiTest : ApiTest() {

    @Test
    fun `a signed-in user renames a city`() = runApiTest {
        val cityId = server.seedCity()
        server.registerStar()
        signIn(server.loginStar())

        val city = postApi(Api.Cities.UpdateCity, CityEdit(cityId, "Mile High")).toDataOrThrow()

        assertEquals("Mile High", city.name, "the response should carry the new name")
        assertEquals("Mile High", nameOf(cityId), "the new name should be stored")
        assertEquals("denver-colorado", city.slug.value, "the slug should survive the rename")
    }

    @Test
    fun `a signed-out user cannot rename a city`() = runApiTest {
        val cityId = server.seedCity()

        postApi(Api.Cities.UpdateCity, CityEdit(cityId, "Mile High")).toProblemOrThrow()

        assertEquals("Denver", nameOf(cityId), "the city should keep its name")
    }

    @Test
    fun `a city cannot take the name of another city in its state`() = runApiTest {
        val cityId = server.seedCity()
        val denver = server.dao.city.readCity(cityId) ?: error("city was not seeded")
        server.dao.city.createCity(denver.copy(name = "Boulder"))
        server.registerStar()
        signIn(server.loginStar())

        postApi(Api.Cities.UpdateCity, CityEdit(cityId, "Boulder")).toProblemOrThrow()

        assertEquals("Denver", nameOf(cityId), "the city should keep its name")
    }

    private suspend fun nameOf(cityId: CityId) = server.dao.city.readCity(cityId)?.name
}
