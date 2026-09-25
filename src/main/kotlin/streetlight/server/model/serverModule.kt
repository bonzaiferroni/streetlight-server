package streetlight.server.model

import kabinet.utils.Environment
import klutch.db.services.SessionService
import klutch.environment.SystemEnvironment
import klutch.environment.readEnvFromPath
import klutch.environment.readEnvFromPathOrNull
import klutch.server.Authorizer
import org.koin.dsl.module
import streetlight.server.db.services.SongTableService
import klutch.server.ProviderScope
import klutch.server.provide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import streetlight.agent.HtmlParserClient
import streetlight.agent.KoogHtmlParserClient
import streetlight.agent.readLmConfig
import streetlight.server.db.services.StarSessionService
import streetlight.server.external.OSMMapReferenceClient
import streetlight.server.external.PostmarkEmailClient
import streetlight.server.routes.LocationParser

/** The server's services, one of each. */
val serverModule = module {
    single { SystemEnvironment.fromSystem(readEnvFromPathOrNull()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { DaoFacade() }

    // clients
    single { OSMMapReferenceClient() } bind MapReferenceClient::class
    single { KoogHtmlParserClient(get<Environment>().readLmConfig()) } bind HtmlParserClient::class
    single { S3BlobClient(get()) } bind BlobClient::class
    single { LocationParser(get()) }
    single { PostmarkEmailClient(get()) } bind EmailClient::class
    single { ClientFacade(get(), get(), get()) }

    // services
    single { StarSessionService() } bind SessionService::class
    single { ConnectionService() }
    single { Authorizer(get()) }
    single { OmniService(get()) }

    // other
    single { SongTableService() }
}

val ProviderScope.dao get() = provide<DaoFacade>()
