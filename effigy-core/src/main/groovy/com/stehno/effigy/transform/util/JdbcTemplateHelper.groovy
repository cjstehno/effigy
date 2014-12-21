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

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement

/**
 * AST helper for building interactions with the Spring JdbcTemplate.
 */
class JdbcTemplateHelper {

    /**
     * Expression used to access the RowMapper accessor method for an entity.
     *
     * @param entityNode the entity node
     * @return the call expression
     */
    static Expression entityRowMapper(ClassNode entityNode) {
        callX(classX(newClass(entityNode)), 'rowMapper')
    }

    static Expression entityCollectionExtractor(ClassNode entityNode) {
        callX(classX(newClass(entityNode)), 'collectionAssociationExtractor')
    }

    static Expression entityExtractor(ClassNode entityNode) {
        callX(classX(newClass(entityNode)), 'associationExtractor')
    }

    static Statement query(String sql, Expression handler, List<Expression> params = []) {
        returnS(
            callX(varX('jdbcTemplate'), 'query', queryArgs(sql, handler, params))
        )
    }

    static Statement queryForObject(String sql, Expression handler, List<Expression> params = []) {
        returnS(
            callX(varX('jdbcTemplate'), 'queryForObject', queryArgs(sql, handler, params))
        )
    }

    private static ArgumentListExpression queryArgs(String sql, Expression handler, List<Expression> params) {
        ArgumentListExpression arguments = args(
            constX(sql),
            handler
        )

        params.each { pex ->
            arguments.addExpression(pex)
        }

        arguments
    }
}
