package streetlight.server.db

import kabinet.utils.Environment
import klutch.db.createCounterTrigger
import klutch.db.createSyncValueTrigger
import klutch.db.services.initUsers
import klutch.server.provide
import klutch.utils.dbLog
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import streetlight.server.db.tables.*
import streetlight.server.model.ServerScope

fun initDb(server: ServerScope) {
    dbLog.logInfo("initializing db")
    val env = server.provide<Environment>()
    val db = connectDb(env)

    transaction(db) {
        // replaced: SchemaUtils.create(*dbTables.toTypedArray())
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables.toTypedArray())
        statements.forEach { statement ->
            exec(statement)
        }

        // uncomment for logger
        // addLogger(StdOutSqlLogger)

        counterTriggers.forEach {
            createCounterTrigger(it)
        }

        syncValueTriggers.forEach {
            createSyncValueTrigger(it)
        }
    }

    runBlocking {
        initUsers(server)
        initPolicy()
    }
}

private val dbTables = listOf(
    LocationTable,
    EventTable,
    SongTable,
    RenditionTable,
    RequestTable,
    SessionTable,
    PerformerTable,
    TransitRouteTable,
    TransitStopTable,
    TransitRouteStopTable,
    EventTagTable,
    UploadFileTable,
    TalentTable,
    GalaxyTable,
    GalaxyHostTable,
    GalaxyCommentTable,
    GalaxyStarTable,
    PostTable,
    PostStarTable,
    CommentTable,
    CommentStarTable,
    MediaCommentTable,
    EventStarTable,
    LocationStarTable,
    CityTable,
    CountryTable,
    StateTable,
    OmniTable,
    EditLogTable,
    TaskTable,
    MediaTable,
    FeedbackTable,
    QuorumTable,
    PolicyTable,
    FlagTable,
    StarTable,
    SiteStatusTable,
)

val counterTriggers get() = listOf(
    cityGalaxyTrigger,
    galaxyStarTrigger,
    galaxyEventCountTrigger,
    galaxyLocationCountTrigger,
    galaxyPostCountTrigger,
    eventStarCountTrigger,
    postStarCountTrigger,
)

val syncValueTriggers get() = listOf(
    stateCountrySync,
    cityStateSync,
    cityCountrySync,
    locationCitySync,
    locationStateSync,
    postUsernameSync,
    postEventLocationSync,
    postGalaxyNameSync,
    postGalaxySlugSync,
    eventLocationSlugSync,
    eventUsernameSync,
    locationHostSync,
    locationScoutSync,
    editUsernameSync,
    mediaUsernameSync,
    feedbackUsernameSync,
)

fun connectDb(env: Environment) = Database.connect(
    url = "jdbc:postgresql://localhost:5432/streetlightdb",
    driver = "org.postgresql.Driver",
    user = "streetlight",
    password = env.read("PSQL_PW")
)