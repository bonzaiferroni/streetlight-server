package streetlight.server.model

import kabinet.console.LogHandle
import kabinet.console.globalConsole
import kabinet.utils.Environment
import klutch.db.services.RefreshTokenService
import klutch.environment.readEnvFromPath
import org.koin.dsl.module
import streetlight.server.db.services.SongTableService
import klutch.server.JwtService
import klutch.server.ProviderScope
import klutch.server.TokenConfig
import klutch.server.provide
import streetlight.agent.ParserClient
import streetlight.server.external.OSMHttpClient
import streetlight.server.plugins.StarRefreshTokenTable
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

    // clients
    single { OSMHttpClient() }
    single { ParserClient(get()) }
    single { BlobClient(get()) }
    single { LocationParser(get()) }
    single { ClientFacade(get(), get(), get()) }

    // services
    single { JwtService(get(), get()) }
    single { RefreshTokenService(StarRefreshTokenTable) }
    single { OmniService(get()) }

    // other
    single { SongTableService() }
}

val ProviderScope.dao get() = provide<DaoFacade>()
