package streetlight.server

import kabinet.utils.Environment
import klutch.db.services.SessionService
import klutch.environment.readEnvFromPath
import klutch.server.Authorizer
import klutch.server.KoinProvider
import klutch.server.provide
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import streetlight.server.db.services.StarSessionService
import streetlight.server.model.EmailRouter
import streetlight.server.model.TestBlobClient
import streetlight.server.model.TestEmailClient
import streetlight.server.model.TestMapClient
import streetlight.server.model.BlobClient
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.EmailClient
import streetlight.server.model.MapClient
import streetlight.server.model.Server
import streetlight.server.model.ServerScope

fun buildTestServer(
    env: Environment = readEnvFromPath(),
    emailRouter: EmailRouter = EmailRouter(),
    daoFacade: DaoFacade = DaoFacade(),
    mapClient: MapClient = TestMapClient(),
    blobClient: BlobClient = TestBlobClient(),
    emailClient: (EmailRouter) -> EmailClient = { TestEmailClient(it) },
): ServerScope {
    val koin = koinApplication {
        modules(module {
            single { env }
            single { emailRouter }
            single { daoFacade }
            single { mapClient }
            single { blobClient }
            single { emailClient(emailRouter) }
            single { ClientFacade(get(), get(), get()) }
            single { StarSessionService() } bind SessionService::class
            single { Authorizer(get()) }
        })
    }.koin

    val provider = KoinProvider(koin)

    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    return Server(provider, dao, client)
}