package streetlight.server.model

import klutch.db.services.SessionService
import klutch.environment.readEnvFromPath
import klutch.server.Authorizer
import org.koin.dsl.module
import streetlight.server.db.services.SongTableService
import klutch.server.ProviderScope
import klutch.server.provide
import org.koin.dsl.bind
import streetlight.agent.ParserClient
import streetlight.server.db.services.StarSessionService
import streetlight.server.external.OSMHttpClient
import streetlight.server.routes.LocationParser

val serverModule = module {
    single { readEnvFromPath() }
    single { DaoFacade() }

    // clients
    single { OSMHttpClient() }
    single { ParserClient(get()) }
    single { BlobClient(get()) }
    single { LocationParser(get()) }
    single { ClientFacade(get(), get(), get()) }

    // services
    single { StarSessionService() } bind SessionService::class
    single { Authorizer(get()) }
    single { OmniService(get()) }

    // other
    single { SongTableService() }
}

val ProviderScope.dao get() = provide<DaoFacade>()
