package streetlight.server

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer
import streetlight.server.db.connectDb
import streetlight.server.db.raiseSchema
import streetlight.server.model.EmailRouter
import streetlight.server.model.ServerScope
import kotlin.test.BeforeTest

abstract class DatabaseTest {
    companion object {
        private val container = PostgreSQLContainer("postgres:18-alpine")
            .withReuse(true)
            .apply { start() }

        val database: Database = connectDb(
            url = container.jdbcUrl,
            user = container.username,
            password = container.password,
        ).also {
            TransactionManager.defaultDatabase = it
            raiseSchema(it)
        }
    }

    protected lateinit var server: TestServer

    @BeforeTest
    fun prepareScenario() {
        truncateAll(database)
        server = buildTestServer()
    }
}

private fun truncateAll(db: Database) {
    transaction(db) {
        val names = exec(
            """
            SELECT tablename FROM pg_tables WHERE schemaname = 'public'
            """
        ) { rs ->
            buildList { while (rs.next()) add(rs.getString(1)) }
        }.orEmpty()

        if (names.isNotEmpty()) {
            exec("TRUNCATE ${names.joinToString(", ")} RESTART IDENTITY CASCADE")
        }
    }
}
