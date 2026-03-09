package streetlight.server.plugins

import kabinet.utils.Environment
import klutch.db.services.UserInitService
import klutch.db.tables.RefreshTokenTable
import klutch.db.tables.UserTable
import klutch.environment.readEnvFromPath
import klutch.utils.dbLog
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.db.tables.AreaTable
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.RequestTable
import streetlight.server.db.tables.SongTable
import streetlight.server.db.tables.RenditionTable
import streetlight.server.db.tables.PerformerTable
import streetlight.server.db.tables.TransitRouteStopTable
import streetlight.server.db.tables.TransitRouteTable
import streetlight.server.db.tables.TransitStopTable
import streetlight.server.db.tables.UploadFileTable
import streetlight.server.db.tables.EventTagTable
import streetlight.server.db.tables.TalentTable
import streetlight.server.db.tables.GalaxyTable

fun initDb(
    // app: ServerProvider = RuntimeProvider
) {
    dbLog.logInfo("initializing db")
    val env = readEnvFromPath()
    val db = connectDb(env)

    transaction(db) {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables.toTypedArray())
        statements.forEach { statement ->
            exec(statement)
        }
        // SchemaUtils.create(*dbTables.toTypedArray())
        // addLogger(StdOutSqlLogger)
    }

    runBlocking {
        UserInitService(env).initUsers()
    }
}

val dbTables = listOf(
    UserTable,
    LocationTable,
    AreaTable,
    EventTable,
    SongTable,
    RenditionTable,
    RequestTable,
    RefreshTokenTable,
    PerformerTable,
    TransitRouteTable,
    TransitStopTable,
    TransitRouteStopTable,
    EventTagTable,
    UploadFileTable,
    TalentTable,
    GalaxyTable,
)

fun connectDb(env: Environment) = Database.connect(
    url = "jdbc:postgresql://localhost:5432/streetlightdb",
    driver = "org.postgresql.Driver",
    user = "streetlight",
    password = env.read("PSQL_PW")
)