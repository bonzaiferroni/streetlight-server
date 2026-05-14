package streetlight.server.model

import kabinet.utils.Environment
import klutch.db.services.RefreshTokenService
import klutch.environment.readEnvFromPath
import klutch.server.ApiContext
import org.koin.dsl.module
import streetlight.server.db.services.SongTableService
import klutch.server.JwtService
import klutch.server.TokenConfig
import streetlight.agent.ParserService
import streetlight.server.external.OSMHttpClient
import streetlight.server.plugins.StarRefreshTokenTable
import streetlight.server.routes.EventParser
import streetlight.server.routes.LocationParser

val serverModule = module {
    single { readEnvFromPath() }
    single { DaoFacade() }
    single { TokenConfig(
        audience = "streetlight-api",
        issuer = "streetlight-auth",
        realm = "streetlight-api",
        lifetimeSeconds = 30 * 60,
    ) }
    single { ObjectStorageClient(get()) }

    // services
    single { ParserService(get()) }
    single { EventParser(get()) }
    single { LocationParser(get()) }
    single { JwtService(get(), get()) }
    single { SongTableService() }
    single { OmniService(get()) }
    single { ContentService(get()) }
    single { RefreshTokenService(StarRefreshTokenTable) }
    single { OSMHttpClient() }
}

val ApiContext.env get() = server.koin.get<Environment>()
val ApiContext.dao get() = server.koin.get<DaoFacade>()

