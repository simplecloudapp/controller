package app.simplecloud.controller.shared.db

import org.jooq.DSLContext
import org.jooq.impl.DSL


class Database {
    companion object {
        @JvmStatic
        private lateinit var instance: DSLContext
        fun init(config: DatabaseConfig) {
            instance = DSL.using(config.toDatabaseUrl())
            setup()
        }

        private fun setup() {
            System.setProperty("org.jooq.no-logo", "true")
            System.setProperty("org.jooq.no-tips", "true")
            val setupInputStream = Database::class.java.getResourceAsStream("/schema.sql")
                ?: throw IllegalArgumentException("Database schema not found.")
            val setupCommands = setupInputStream.bufferedReader().use { it.readText() }.split(";")
            setupCommands.forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty())
                    instance.execute(DSL.sql(trimmed))
            }
        }

        fun get(): DSLContext {
            return instance
        }
    }
}