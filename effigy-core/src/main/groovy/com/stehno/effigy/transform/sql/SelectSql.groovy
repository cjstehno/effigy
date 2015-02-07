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
 * Builder for select SQL queries. This class should not be used directly, see the SqlBuilder instead.
 */
@SuppressWarnings('ConfusingMethodName')
class SelectSql implements Predicated<SelectSql> {

    private static final String COMMA_SPACE = ', '
    private static final String SPACE = ' '
    private final froms = []
    private final columns = []
    private final leftOuterJoins = []

    private final wheres = []
    private final params = []

    private String limit
    private Expression limitParam

    private String offset
    private Expression offsetParam

    private final orders = []

    static SelectSql select() {
        new SelectSql()
    }

    @Override
    List<Expression> getParams() {
        def result = []
        result.addAll(params)
        if (limitParam) result.add(limitParam)
        if (offsetParam) result.add(offsetParam)
        result
    }

    SelectSql columns(List<String> columnNames) {
        columns.addAll(columnNames)
        this
    }

    SelectSql column(String name) {
        columns << name
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

    SelectSql wheres(List<String> criteria, List<Expression> paramXs) {
        if (criteria) {
            wheres.addAll(criteria)
            params.addAll(paramXs)
        }
        this
    }

    SelectSql where(String criteria, List<Expression> paramXs) {
        if (criteria) {
            wheres.add(criteria)
            params.addAll(paramXs)
        }
        this
    }

    SelectSql where(String criteria, Expression paramX) {
        wheres << criteria
        params << paramX
        this
    }

    SelectSql limit(String value) {
        limit = value
        this
    }

    SelectSql limit(String value, Expression exp) {
        limit = value
        limitParam = exp
        this
    }

    SelectSql offset(String value, Expression exp) {
        offset = value
        offsetParam = exp
        this
    }

    SelectSql order(String ordering) {
        orders << ordering
        this
    }

    SelectSql orders(List<String> orders) {
        this.orders.addAll(orders)
        this
    }

    String build() {
        StringBuilder sql = new StringBuilder('select ')

        sql.append(columns.join(COMMA_SPACE))

        sql.append(' from ')

        sql.append(froms.join(COMMA_SPACE)).append(SPACE)

        sql.append(leftOuterJoins.join(SPACE))

        if (wheres) {
            sql.append(' where ')
            sql.append(wheres.join(' and '))
        }

        if (orders) {
            sql.append(' order by ')
            sql.append(orders.join(','))
        }

        if (limit) {
            sql.append(" limit $limit")
        }

        if (offset) {
            sql.append(" offset $offset")
        }

        sql.toString()
    }
}

interface Parametized {

    List<Expression> getParams()
}

interface Predicated<T> extends Parametized {

    T wheres(List<String> criteria, List<Expression> params)

    T where(String criteria, Expression param)

    T where(String criteria, List<Expression> paramXs)
}
