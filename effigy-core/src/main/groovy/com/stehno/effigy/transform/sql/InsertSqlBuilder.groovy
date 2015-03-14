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

package com.stehno.effigy.transform.sql

import org.codehaus.groovy.ast.expr.Expression

/**
 * Sql builder for building insert statements.
 */
@SuppressWarnings('ConfusingMethodName')
class InsertSqlBuilder {

    private static final String COMMA = ','
    private String table

    private final columns = []
    private final columnValues = []

    static InsertSqlBuilder insert() {
        new InsertSqlBuilder()
    }

    List<Expression> getValues() { columnValues.asImmutable() }

    InsertSqlBuilder table(String table) {
        this.table = table
        this
    }

    InsertSqlBuilder column(String colname, Expression value) {
        columns << colname
        columnValues << value
        this
    }

    String build() {
        StringBuilder sql = new StringBuilder('insert into ')

        sql.append(table)

        sql.append(' (').append(columns.join(COMMA)).append(') values (')
            .append(columns.collect { '?' }.join(COMMA))
            .append(')')

        sql.toString()
    }
}
