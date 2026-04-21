package streetlight.server.db

import kabinet.utils.Environment
import klutch.db.services.UserInitService
import klutch.db.tables.RefreshTokenTable
import klutch.environment.readEnvFromPath
import klutch.utils.dbLog
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import streetlight.server.db.services.StarAuthDao
import streetlight.server.db.services.provideStarUser
import streetlight.server.db.tables.*

fun initDb(
    // app: ServerProvider = RuntimeProvider
    refreshTokenTable: RefreshTokenTable
) {
    dbLog.logInfo("initializing db")
    val env = readEnvFromPath()
    val db = connectDb(env)

    transaction(db) {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables(refreshTokenTable).toTypedArray())
        statements.forEach { statement ->
            exec(statement)
        }
        // SchemaUtils.create(*dbTables.toTypedArray())
        // addLogger(StdOutSqlLogger)
    }

    runBlocking {
        UserInitService(env, StarAuthDao(), ::provideStarUser).initUsers()
    }
}

fun dbTables(refreshTokenTable: RefreshTokenTable) = listOf(
    LocationTable,
    EventTable,
    SongTable,
    RenditionTable,
    RequestTable,
    refreshTokenTable,
    PerformerTable,
    TransitRouteTable,
    TransitStopTable,
    TransitRouteStopTable,
    EventTagTable,
    UploadFileTable,
    TalentTable,
    GalaxyTable,
    EventPostTable,
    LocationPostTable,
    GalaxyLocationPostTable,
    GalaxyLightTable,
    EventLightTable,
    OmniTable,
)

fun connectDb(env: Environment) = Database.connect(
    url = "jdbc:postgresql://localhost:5432/streetlightdb",
    driver = "org.postgresql.Driver",
    user = "streetlight",
    password = env.read("PSQL_PW")
)