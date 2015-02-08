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
 * Builder used to build "delete" sql queries. For internal use.
 */
@SuppressWarnings('ConfusingMethodName')
class DeleteSql implements Predicated<DeleteSql> {

    private String from

    static DeleteSql delete() {
        new DeleteSql()
    }

    DeleteSql from(String value) {
        from = value
        this
    }

    String build() {
        StringBuilder sql = new StringBuilder('delete from ')
        sql.append(from)

        if (wheres) {
            sql.append(' where ')
            sql.append(wheres.join(' and '))
        }

        sql.toString()
    }
}
