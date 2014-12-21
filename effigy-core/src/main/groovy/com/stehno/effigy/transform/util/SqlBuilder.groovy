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

package com.stehno.effigy.transform.util

/**
 * Created by cjstehno on 12/21/2014.
 */
class SqlBuilder {

    static SelectSql select() {
        new SelectSql()
    }
}

class SelectSql {

    private final froms = []
    private final columns = []
    private final leftOuterJoins = []
    private final wheres = []

    SelectSql columns(List<String> columnNames) {
        columnNames.each { name ->
            column(name)
        }
        this
    }

    SelectSql column(String name) {
        columns << "$name"
        this
    }

    SelectSql column(String name, String alias) {
        columns << "$name as $alias"
        this
    }

    SelectSql column(String table, String name, String alias) {
        columns << "$table.$name as $alias"
        this
    }

    SelectSql from(String table, String alias = null) {
        froms << "$table${alias ? " as $alias" : ''}"
        this
    }

    SelectSql leftOuterJoin(String joinTable, String tableA, String tableAId, String tableB, String tableBId) {
        leftOuterJoins << "LEFT OUTER JOIN $joinTable on $tableA.$tableAId=$tableB.$tableBId"
        this
    }

    SelectSql where(String criteria) {
        wheres << criteria
        this
    }

    String build() {
        StringBuilder sql = new StringBuilder('select ')

        sql.append(columns.join(', '))

        sql.append(' from ')

        sql.append(froms.join(', ')).append(' ')

        sql.append(leftOuterJoins.join(' '))

        if (wheres) {
            sql.append(' where ')
            sql.append(wheres.join(' and '))
        }

        sql.toString()
    }
}
