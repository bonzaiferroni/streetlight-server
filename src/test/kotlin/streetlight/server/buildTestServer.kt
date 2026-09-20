package streetlight.server

import kabinet.utils.Environment
import kampfire.api.EmailAddress
import klutch.db.services.SessionService
import klutch.server.Authorizer
import klutch.server.KoinProvider
import klutch.server.ProviderScope
import klutch.server.provide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import streetlight.agent.HtmlParserClient
import streetlight.server.model.TestHtmlParserClient
import streetlight.server.db.services.SongTableService
import streetlight.server.db.services.StarSessionService
import streetlight.server.model.AppEmail
import streetlight.server.model.ConnectionService
import streetlight.server.model.EmailRouter
import streetlight.server.model.OmniService
import streetlight.server.model.TestBlobClient
import streetlight.server.model.TestEmailClient
import streetlight.server.model.TestMapReferenceClient
import streetlight.server.model.BlobClient
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.EmailClient
import streetlight.server.model.MapReferenceClient
import streetlight.server.model.Server
import streetlight.server.model.ServerConfig
import streetlight.server.model.ServerScope
import streetlight.server.routes.LocationParser

fun buildTestServer(
    env: Environment = readTestEnvironment(),
    serverConfig: ServerConfig = ServerConfig(
        withMetrics = false,
        withDatabase = false,
        withTransit = false,
        withRateLimits = false,
    ),
    emailRouter: EmailRouter = EmailRouter(),
    daoFacade: DaoFacade = DaoFacade(),
    mapClient: MapReferenceClient = TestMapReferenceClient(),
    htmlParserClient: HtmlParserClient = TestHtmlParserClient(),
    blobClient: BlobClient = TestBlobClient(),
    emailClient: (EmailRouter) -> EmailClient = { TestEmailClient(it) },
): TestServer {
    val koin = koinApplication {
        modules(module {
            single { env }
            single { serverConfig }
            single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
            single { emailRouter }
            single { daoFacade }
            single { mapClient }
            single { blobClient }
            single { emailClient(emailRouter) }
            single { ClientFacade(get(), get(), get()) }
            single { StarSessionService() } bind SessionService::class
            single { Authorizer(get()) }
            single { ConnectionService() }
            single { OmniService(get()) }
            single { htmlParserClient }
            single { LocationParser(get()) }
            single { SongTableService() }
        })
    }.koin

    val provider = KoinProvider(koin)

    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    val authorizer = provider.provide<Authorizer>()
    return TestServer(provider, dao, client, authorizer, emailRouter)
}

class TestServer(
    provider: ProviderScope,
    override val dao: DaoFacade,
    override val client: ClientFacade,
    val authorizer: Authorizer,
    val emailRouter: EmailRouter,
): ServerScope, ProviderScope by provider {

    override val appEmail = AppEmail (
        EmailAddress("info@streetlight.ing"),
        EmailAddress("support@streetlight.ing")
    )
}
