package streetlight.server

import klutch.environment.SystemEnvironment
import klutch.environment.readEnvFromPathOrNull
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import streetlight.server.db.dbTables

//@OptIn(ExperimentalDatabaseMigrationApi::class)
//fun main() {
//    val env = SystemEnvironment.fromSystem(readEnvFromPathOrNull())
//    Flyway.configure()
//        .dataSource(env.read("DB_URL"), env.read("DB_USER"), env.read("DB_PASSWORD"))
//        .baselineVersion("1")
//        .load()
//        .baseline()
//}