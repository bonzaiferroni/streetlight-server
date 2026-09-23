@file:OptIn(ExperimentalDatabaseMigrationApi::class)

package streetlight.server.tools

import klutch.environment.SystemEnvironment
import klutch.environment.readEnvFromPathOrNull
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import streetlight.server.EnvKey
import streetlight.server.db.dbTables
import java.io.File
import java.sql.DriverManager
import kotlin.system.exitProcess

private const val MIGRATION_DIR = "src/main/resources/db/migration"
private const val SHADOW_DB = "streetlight_shadow"

private const val CHECK_FLAG = "--check"

fun main(args: Array<String>) {
    val description = args.firstOrNull() ?: error("usage: generateMigration <description> | $CHECK_FLAG")
    val isCheck = description == CHECK_FLAG
    val env = SystemEnvironment.fromSystem(readEnvFromPathOrNull())
    val user = env.read(EnvKey.DB_USER)
    val password = env.read(EnvKey.DB_PASSWORD)
    val base = env.read(EnvKey.DB_URL).substringBeforeLast('/')

    fun admin(sql: String) = DriverManager.getConnection("$base/postgres", user, password)
        .use { it.createStatement().use { statement -> statement.execute(sql) } }

    admin("drop database if exists $SHADOW_DB")
    admin("create database $SHADOW_DB")
    var pending = emptyList<String>()
    try {
        Flyway.configure()
            .dataSource("$base/$SHADOW_DB", user, password)
            .locations("filesystem:$MIGRATION_DIR")
            .load()
            .migrate()

        val db = Database.connect("$base/$SHADOW_DB", driver = "org.postgresql.Driver", user = user, password = password)
        if (isCheck) {
            pending = transaction(db) {
                MigrationUtils.statementsRequiredForDatabaseMigration(*dbTables.toTypedArray(), withLogs = false)
            }
            if (pending.isEmpty()) println("no schema changes since the last migration")
            else pending.forEach { println(it) }
            return
        }
        val name = "V${nextMigrationVersion()}__$description"
        transaction(db) {
            MigrationUtils.generateMigrationScript(
                *dbTables.toTypedArray(),
                scriptDirectory = MIGRATION_DIR,
                scriptName = name,
                withLogs = false,
            )
        }
        val script = File("$MIGRATION_DIR/$name.sql")
        if (script.readText().isBlank()) {
            script.delete()
            println("no schema changes since the last migration")
        } else {
            println("wrote ${script.path}")
        }
    } finally {
        admin("drop database if exists $SHADOW_DB")
        if (pending.isNotEmpty()) exitProcess(1)
    }
}

private fun nextMigrationVersion(): Int = File(MIGRATION_DIR).listFiles()
    .orEmpty()
    .mapNotNull { Regex("""^V(\d+)__""").find(it.name)?.groupValues?.get(1)?.toInt() }
    .maxOrNull()
    ?.plus(1) ?: 1