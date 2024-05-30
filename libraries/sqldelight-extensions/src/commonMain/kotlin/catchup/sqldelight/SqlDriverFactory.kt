package catchup.sqldelight

import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/** A factory for creating [SqlDriver] instances. Allows for IoC in downstream users. */
fun interface SqlDriverFactory {
  fun create(schema: SqlSchema<Value<Unit>>, name: String?): SqlDriver
}
