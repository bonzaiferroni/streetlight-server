package streetlight.server.model

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import kabinet.clients.ReplicateClient
import kabinet.clients.ReplicateInput
import kabinet.console.globalConsole
import kabinet.utils.Environment
import klutch.db.services.UserTableDao
import klutch.db.services.UserTableService
import klutch.environment.readEnvFromPath
import klutch.gemini.GeminiService
import klutch.gemini.SpeechService
import streetlight.agent.UrlParser
import streetlight.server.db.services.*

interface Streetlight {
    val env: Environment
    val dao: DaoFacade
    val service: ServiceFacade
    val ai: InferenceFacade
    val storage: StorageFacade
}

class DaoFacade(
    val location: LocationTableDao = LocationTableDao(),
    val galaxy: GalaxyTableDao = GalaxyTableDao(),
    val star: StarTableDao = StarTableDao(),
    val eventPost: EventPostTableDao = EventPostTableDao(),
    val locationPost: LocationPostTableDao = LocationPostTableDao(),
    val song: SongTableDao = SongTableDao(),
    val event: EventTableDao = EventTableDao(),
    val rendition: RenditionTableDao = RenditionTableDao(),
    val user: UserTableDao = UserTableDao(),
    val spark: SparkTableDao = SparkTableDao(),
    val talent: TalentTableDao = TalentTableDao(),
    val request: RequestTableDao = RequestTableDao(),
    val guest: GuestTableDao = GuestTableDao(),
    val transitRoute: TransitRouteTableDao = TransitRouteTableDao(),
    val transitStop: TransitStopTableDao = TransitStopTableDao(),
    val userFile: UploadFileTableDao = UploadFileTableDao()
)

class ServiceFacade(
    app: Streetlight,
    val song: SongTableService = SongTableService(),
    val service: UserTableService = UserTableService(),
)

class StorageFacade(
    env: Environment
) {
    val s3 = S3Client {
        region = env.read("S3_REGION") // e.g. "gra" or "sbg"
        endpointUrl = Url.parse(env.read("S3_ENDPOINT_URL"))
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = env.read("S3_ACCESS_KEY")
            secretAccessKey = env.read("S3_SECRET_KEY")
        }
    }
}

class InferenceFacade(
    env: Environment
) {
    val parser = UrlParser(env.read("GEMINI_KEY_A"))

    private val failedRequestFilter = mutableSetOf<String>()
    private val replicate = ReplicateClient(env.read("REPLICATE_KEY"))
    private val serverIp = env.read("SERVER_IP")
    private val speechPort = env.read("SERVER_PORT")

    @Deprecated("move GeminiService to another package ")
    val gemini = GeminiService(env)
    @Deprecated("move SpeechService to another package ")
    val speech = SpeechService { request ->
        val filename = request.toFilename()
        if (failedRequestFilter.contains(filename)) return@SpeechService null
        val model = "lucataco/orpheus-3b-0.1-ft:79f2a473e6a9720716a473d9b2f2951437dbf91dc02ccb7079fb3d89b881207f"
        val request = ReplicateInput(
            text = request.text,
            voice = request.voice,
            maxNewTokens = 2000
        )
        replicate.requestBytes(
            url = "http://$serverIp:$speechPort/speech",
            input = request
        ).also {
            if (it == null) console.log("holocene unable to generate speech")
        } ?: replicate.requestFileBytes(model, request).also {
            if (it == null) console.log("replicate unable to generate speech").also { failedRequestFilter.add(filename)}
        }
    }
}

fun createStreetlight(): Streetlight {
    return object: Streetlight {
        override val env = readEnvFromPath()
        override val dao = DaoFacade()
        override val ai = InferenceFacade(env)
        override val storage = StorageFacade(env)
        override val service = ServiceFacade(this)
    }
}

private val console = globalConsole.getHandle(Streetlight::class)
