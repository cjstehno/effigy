/*
 * Copyright (c) 2015 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stehno.effigy.test

import org.junit.rules.ExternalResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder

import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2

/**
 * Simple wrapper around the spring embedded database builder to provide a reusable test database configuration
 * in unit testing.
 */
class DatabaseEnvironment extends ExternalResource {

    String schema
    String data

    private EmbeddedDatabase database

    private JdbcTemplate jdbc

    JdbcTemplate getJdbcTemplate() { jdbc }

    @Override
    protected void before() throws Throwable {
        database = new EmbeddedDatabaseBuilder()
            .setType(H2)
            .ignoreFailedDrops(true)
            .addScripts(schema, data)
            .build()

        jdbc = new JdbcTemplate(new SingleConnectionDataSource(database.connection, false))
    }

    @Override
    protected void after() {
        database.shutdown()
    }
}
