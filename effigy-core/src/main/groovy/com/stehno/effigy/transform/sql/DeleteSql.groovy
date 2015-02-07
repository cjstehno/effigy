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
 * Builder used to build "delete" sql queries. For internal use.
 */
@SuppressWarnings('ConfusingMethodName')
class DeleteSql implements Predicated<DeleteSql> {

    private String from
    private final wheres = []
    private final params = []

    static DeleteSql delete() {
        new DeleteSql()
    }

    @Override
    List<Expression> getParams() {
        params.asImmutable()
    }

    DeleteSql from(String value) {
        from = value
        this
    }

    @Override
    DeleteSql wheres(List<String> criteria, List<Expression> paramXs) {
        if (criteria) {
            wheres.addAll(criteria)
            params.addAll(paramXs)
        }
        this
    }

    @Override
    DeleteSql where(String criteria, Expression paramX) {
        wheres << criteria
        params << paramX
        this
    }

    @Override
    DeleteSql where(String criteria, List<Expression> paramXs) {
        wheres << criteria
        params.addAll(paramXs)
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
