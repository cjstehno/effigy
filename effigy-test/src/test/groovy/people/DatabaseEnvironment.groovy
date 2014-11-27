package people
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2

import org.junit.rules.ExternalResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
/**
 * Created by cjstehno on 11/26/2014.
 */
class DatabaseEnvironment extends ExternalResource {

    private EmbeddedDatabase database

    JdbcTemplate jdbcTemplate

    @Override
    protected void before() throws Throwable {
        database = new EmbeddedDatabaseBuilder().setType(H2)
            .ignoreFailedDrops(true)
            .addScript('schema.sql')
            .build()

        jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(database.connection, false))
    }

    @Override
    protected void after() {
        database.shutdown()
    }
}
