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

    raiseSchema(db)

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
    ImageTable,
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
    AuthTokenTable,
    BouncedEmailTable,
    OriginTable,
    ParserTable,
    LocationOriginTable,
    LinkTable,
    LinkAliasTable,
    SubdomainTable,
)

private val counterTriggers get() = listOf(
    cityGalaxyTrigger,
    galaxyStarTrigger,
    galaxyEventCountTrigger,
    galaxyLocationCountTrigger,
    galaxyPostCountTrigger,
    eventStarCountTrigger,
    postStarCountTrigger,
)

private val syncValueTriggers get() = listOf(
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
    messageAuthorSync,
)

fun connectDb(url: String, user: String, password: String) = Database.connect(
    url = url,
    driver = "org.postgresql.Driver",
    user = user,
    password = password,
)

fun connectDb(env: Environment) = connectDb(
    url = "jdbc:postgresql://localhost:5432/streetlightdb",
    user = "streetlight",
    password = env.read("PSQL_PW"),
)

fun raiseSchema(db: Database) {
    transaction(db) {
        MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables.toTypedArray())
            .forEach { exec(it) }

        counterTriggers.forEach { createCounterTrigger(it) }
        syncValueTriggers.forEach { createSyncValueTrigger(it) }
    }
}