package streetlight.server.routes

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.environment.readEnvFromPath
import klutch.server.getEndpoint
import klutch.server.readParam
import streetlight.agent.UrlParser
import streetlight.model.Api
import streetlight.server.RuntimeProvider
import streetlight.server.ServerProvider

private val console = globalConsole.getHandle(Routing::servePosts.name)

fun Routing.servePosts(app: ServerProvider = RuntimeProvider) {
    val env = readEnvFromPath()
    // Get an API key from the OPENAI_API_KEY environment variable
    val apiKey = env.read("GEMINI_KEY_A")
    val agent = UrlParser(apiKey)

//    getEndpoint(Api.Stories.ReadUrl) { endpoint ->
//        val url = readParam(endpoint.url)
//        agent.readUrl(url, postInstructions)
//    }
}

const val postInstructions = "Read the following html. It is a user post or a news article. " +
        "Your job is to extract the requested information as json. " +
        "Provide a headline that describes the story, if one is not present in the content then generate an appropriate headline. " +
        "Provide a brief description of the story, one or two sentences. " +
        "Provide the url of any feature image that can be identified with meta information or given its placement in the body. " +
        "If the content refers to a location, provide an estimate of the location (longitude/latitude). " +
        "Provide the time that the story was posted. "


//    val name: String? = null,
//    val time: LocalTime? = null,
//    val date: LocalDate? = null,
//    val location: String? = null,
//    val address: String? = null,
//    val imageUrl: String? = null,
//    val description: String? = null,