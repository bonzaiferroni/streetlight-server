package streetlight.server.model

import streetlight.server.service.ConnectionService
import streetlight.server.service.OmniService
import kabinet.utils.Environment
import klutch.db.services.SessionService
import klutch.environment.SystemEnvironment
import klutch.environment.readEnvFromPath
import klutch.environment.readEnvFromPathOrNull
import klutch.server.Authorizer
import org.koin.dsl.module
import streetlight.server.db.dao.SongDao
import klutch.server.ProviderScope
import klutch.server.provide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import streetlight.server.db.dao.StarSessionDao
import streetlight.server.external.OSMMapReferenceClient
import streetlight.server.external.PostmarkEmailClient
import streetlight.server.external.HttpImageClient
import streetlight.server.external.StripePaymentClient

/** The server's services, one of each. */
val serverModule = module {
    single { SystemEnvironment.fromSystem(readEnvFromPathOrNull()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { DaoFacade() }

    // clients
    single { OSMMapReferenceClient() } bind MapReferenceClient::class
    single { S3BlobClient(get()) } bind BlobClient::class
    single { PostmarkEmailClient(get()) } bind EmailClient::class
    single { StripePaymentClient(get()) } bind PaymentClient::class
    single { HttpImageClient() } bind ImageClient::class
    single { ClientFacade(get(), get(), get(), get(), get()) }

    // services
    single { StarSessionDao() } bind SessionService::class
    single { ConnectionService() }
    single { Authorizer(get()) }
    single { OmniService(get()) }

    // other
    single { SongDao() }
}

val ProviderScope.dao get() = provide<DaoFacade>()
