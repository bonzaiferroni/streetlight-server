package streetlight.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kabinet.utils.Environment
import klutch.db.createCounterTrigger
import klutch.db.createSyncValueTrigger
import klutch.db.services.initUsers
import klutch.server.provide
import klutch.utils.dbLog
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import streetlight.server.BuildMode
import streetlight.server.EnvKey
import streetlight.server.buildMode
import streetlight.server.db.tables.*
import streetlight.server.model.ServerScope

/**
 * Connects the database and brings its schema up to date, then seeds the users and policies.
 *
 * In development the schema is raised from the table definitions; in production Flyway migrates it and only the
 * triggers are installed.
 */
fun initDb(server: ServerScope) {
    dbLog.logInfo("initializing db")
    val env = server.provide<Environment>()
    val db = connectDb(env)

    if (env.buildMode == BuildMode.Development) {
        raiseSchema(db)
    } else {
        migrateSchema(env)
        installTriggers(db)
    }

    runBlocking {
        initUsers(server)
        initPolicy()
    }
}

/** Creates the triggers that keep counts and copied values in sync. */
fun installTriggers(db: Database) {
    transaction(db) {
        counterTriggers.forEach { createCounterTrigger(it) }
        syncValueTriggers.forEach { createSyncValueTrigger(it) }
    }
}

internal val dbTables = listOf(
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
    BugTable,
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
    MessageTable,
    ChatTable,
    ChatStarTable,
    GalaxyMarkTable,
    PostMarkTable,
    PostMarkCountTable,
    SiteEventTable,
)

private val counterTriggers get() = listOf(
    cityGalaxyTrigger,
    cityLocationCountTrigger,
    galaxyStarTrigger,
    galaxyLocationCountTrigger,
    galaxyPostCountTrigger,
    eventStarCountTrigger,
    postStarCountTrigger,
    locationStarCountTrigger,
)

private val syncValueTriggers get() = listOf(
    stateCountrySync,
    cityStateSync,
    cityCountrySync,
    locationCitySync,
    galaxyCitySync,
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

fun connectDb(url: String, user: String, password: String) = Database.connect(
    url = url,
    driver = "org.postgresql.Driver",
    user = user,
    password = password,
)

/** Connects a pool to the database of [env]. */
fun connectDb(env: Environment): Database {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = env.read(EnvKey.DB_URL)
        username = env.read(EnvKey.DB_USER)
        password = env.read(EnvKey.DB_PASSWORD)
        maximumPoolSize = 10
    })

    return Database.connect(dataSource)
}

/** Runs the pending Flyway migrations on the database of [env]. */
fun migrateSchema(env: Environment) {
    Flyway.configure()
        .dataSource(env.read(EnvKey.DB_URL), env.read(EnvKey.DB_USER), env.read(EnvKey.DB_PASSWORD))
        .load()
        .migrate()
}

/** Alters the schema to match the table definitions, and creates the triggers. */
fun raiseSchema(db: Database) {
    transaction(db) {
        MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables.toTypedArray())
            .forEach { exec(it) }

        counterTriggers.forEach { createCounterTrigger(it) }
        syncValueTriggers.forEach { createSyncValueTrigger(it) }
    }
}