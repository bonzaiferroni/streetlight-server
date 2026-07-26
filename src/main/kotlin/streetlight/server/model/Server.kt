package streetlight.server.model

import kabinet.clients.ReplicateClient
import kabinet.clients.ReplicateInput
import kabinet.console.globalConsole
import kabinet.utils.Environment
import klutch.gemini.GeminiService
import klutch.gemini.SpeechService
import klutch.server.ProviderScope
import streetlight.agent.ParserClient
import streetlight.server.db.services.*
import streetlight.server.external.OSMHttpClient
import streetlight.server.external.PostmarkClient

class Server(
    provider: ProviderScope,
    override val dao: DaoFacade,
    override val client: ClientFacade,
): ServerScope, ProviderScope by provider

class DaoFacade(
    val location: LocationTableDao = LocationTableDao(),
    val galaxy: GalaxyTableDao = GalaxyTableDao(),
    val star: StarTableDao = StarTableDao(),
    val post: PostTableDao = PostTableDao(),
    val song: SongTableDao = SongTableDao(),
    val event: EventTableDao = EventTableDao(),
    val rendition: RenditionTableDao = RenditionTableDao(),
    // val user: UserTableDao = UserTableDao(),
    val spark: SparkTableDao = SparkTableDao(),
    val talent: TalentTableDao = TalentTableDao(),
    val request: RequestTableDao = RequestTableDao(),
    val guest: GuestTableDao = GuestTableDao(),
    val transitRoute: TransitRouteTableDao = TransitRouteTableDao(),
    val city: CityTableDao = CityTableDao(),
    val transitStop: TransitStopTableDao = TransitStopTableDao(),
    val userFile: UploadFileTableDao = UploadFileTableDao(),
    val talk: CommentTableDao = CommentTableDao(),
    val omni: OmniTableDao = OmniTableDao(),
    val light: LightTableDao = LightTableDao(),
    val editLog: EditLogTableDao = EditLogTableDao(),
    val quorum: QuorumTableDao = QuorumTableDao(),
    val review: TaskTableDao = TaskTableDao(),
    val media: MediaTableDao = MediaTableDao(),
    val feedback: FeedbackTableDao = FeedbackTableDao(),
    val siteStatus: SiteStatusTableDao = SiteStatusTableDao(),
    val authToken: AuthTokenTableDao = AuthTokenTableDao(),
    val bouncedEmail: BouncedEmailTableDao = BouncedEmailTableDao(),
)

class ClientFacade(
    val blob: BlobClient,
    val osm: OSMHttpClient,
    val parser: ParserClient,
    val postmark: PostmarkClient
)

class InferenceFacade(
    env: Environment
) {
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

private val console = globalConsole.getHandle("StreetlightServer")
