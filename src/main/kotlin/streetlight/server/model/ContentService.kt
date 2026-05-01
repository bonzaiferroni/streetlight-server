package streetlight.server.model

import streetlight.model.data.HomeContent

class ContentService(private val dao: DaoFacade) {

    suspend fun readHomeContent(): HomeContent {
        val posts = dao.post.readActivePosts()
        val galaxies = dao.galaxy.readTopGalaxies(3)
        return HomeContent(
            galaxies = galaxies,
            posts = posts,
        )
    }
}