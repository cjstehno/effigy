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

import groovy.transform.ToString
import org.codehaus.groovy.ast.expr.Expression

/**
 * SQL builder used for working with raw SQL statements and parameters.
 */
@ToString(includeFields = true, includeNames = true)
class RawSqlBuilder {

    private static final VARIABLE_PATTERN = /:[A-Za-z0-9]*/
    private final String text
    private final paramExpressions = [:] as Map<String, Expression>

    /**
     * Creates the SQL builder with the given SQL String.
     *
     * @param sql SQL string with named replacement parameters rather than question marks.
     */
    private RawSqlBuilder(final String sql) {
        this.text = sql;
    }

    static RawSqlBuilder rawSql(String string) {
        new RawSqlBuilder(string)
    }

    RawSqlBuilder param(String name, Expression exp) {
        paramExpressions.put(name, exp)
        this
    }

    /**
     * Replaces the input variables with placeholders (?) and returns the SQL string.
     *
     * @return the prepared SQL string
     */
    String build() {
        text.replaceAll(VARIABLE_PATTERN, '?')
    }

    /**
     * Retrieves the SQL replacement parameters in the order that they appear in the SQL statement (potentially duplicated).
     *
     * @return the correctly ordered list of input parameter expressions
     */
    List<Expression> getParams() {
        def inputParams = []

        text.findAll(VARIABLE_PATTERN).each { varName ->
            def varParam = paramExpressions[varName[1..(-1)]]
            if (varParam) {
                inputParams << varParam
            } else {
                throw new IllegalArgumentException("SQL replacement variable names must match method argument names: '${varName[1..(-1)]}' argument missing.")
            }
        }

        inputParams.asImmutable()
    }
}
