package streetlight.server.model

import streetlight.model.data.HomeContent
import streetlight.model.data.StarId

class ContentService(private val dao: DaoFacade) {

    suspend fun readHomeContent(starId: StarId?): HomeContent {
        val posts = dao.post.readOrderedPosts(starId)
        val galaxies = dao.galaxy.readTopGalaxies(3)
        return HomeContent(
            galaxies = galaxies,
            posts = posts,
        )
    }
}