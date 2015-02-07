/*
 * Copyright (c) 2014 Christopher J. Stehno
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
 * Sql builder for working with update sql statements. For internal use.
 */
@SuppressWarnings('ConfusingMethodName')
class UpdateSql implements Predicated<UpdateSql> {

    private String table

    private final sets = []
    private final setterParams = []

    static UpdateSql update() {
        new UpdateSql()
    }

    @Override
    List<Expression> getParams() {
        def result = []
        if (setterParams) result.addAll(setterParams)
        result.addAll(getWhereParams())
        result
    }

    UpdateSql table(String table) {
        this.table = table
        this
    }

    UpdateSql sets(List<String> values, List<Expression> exps) {
        sets.addAll(values)
        setterParams.addAll(exps)
        this
    }

    UpdateSql set(String value, Expression exp) {
        sets << value
        setterParams << exp
        this
    }

    String build() {
        StringBuilder sql = new StringBuilder('update ')

        sql.append(table).append(' set ')

        sql.append(sets.join(', ')).append(' ')

        if (wheres) {
            sql.append(' where ')
            sql.append(wheres.join(' and '))
        }

        sql.toString()
    }
}
