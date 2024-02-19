package app.simplecloud.controller.shared.db

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.Connection


class Database {
    companion object {
        @JvmStatic
        private lateinit var instance: DSLContext
        fun init(connection: Connection, dialect: SQLDialect = SQLDialect.SQLITE) {
            instance = DSL.using(connection, dialect)
        }

        fun init(url: String, username: String, password: String) {
            instance = DSL.using(url, username, password)
        }

        fun get(): DSLContext {
            return instance
        }
    }
}