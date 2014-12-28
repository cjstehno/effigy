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

/**
 * Created by cjstehno on 12/28/14.
 */
@SuppressWarnings('ConfusingMethodName')
class UpdateSql {

    private String table
    private final sets = []
    private final wheres = []

    UpdateSql table(String table) {
        this.table = table
        this
    }

    UpdateSql sets(List<String> values) {
        sets.addAll(values)
        this
    }

    UpdateSql set(String value) {
        sets << value
        this
    }

    UpdateSql wheres(List<String> criteria) {
        if (criteria) {
            wheres.addAll(criteria)
        }
        this
    }

    UpdateSql where(String criteria) {
        wheres << criteria
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
