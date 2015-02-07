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

import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.EntityPropertyModel
import com.stehno.effigy.transform.model.IdentifierPropertyModel
import org.codehaus.groovy.ast.ClassNode

import static com.stehno.effigy.logging.Logger.trace
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.SelectSql.select

/**
 *  FIXME: document
 */
class RetrievalSql {

    private static final String SEPARATOR = '------------------------------'

    static String selectWithoutAssociations(
        ClassNode entityNode, List<String> whereCriteria = [], String limit = null, String offset = null, String orders = null
    ) {
        SelectSql sql = select().columns(listColumnNames(entityNode)).from(entityTable(entityNode))

        sql.wheres(whereCriteria)

        if (offset) {
            sql.offset(offset)
        }

        if (limit) {
            sql.limit(limit)
        }

        if (orders) {
            sql.order(orders)
        }

        def string = sql.build()

        logSql(entityNode, string)

        string
    }

    static String selectWithAssociations(ClassNode entityNode, List<String> whereCriteria = [], String orders = null) {
        SelectSql selectSql = select()

        String entityTableName = entityTable(entityNode)

        // add entity columns
        entityProperties(entityNode).each { p ->
            addColumns(selectSql, p, entityTableName, entityTableName)
        }

        // add association cols
        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)

            entityProperties(ap.associatedType).each { p ->
                addColumns(selectSql, p, associatedTable, ap.propertyName)
            }
        }

        // add component cols
        components(entityNode).each { ap ->
            entityProperties(ap.type).each { p ->
                selectSql.column(ap.lookupTable, p.columnName as String, "${ap.propertyName}_${p.columnName}")
            }
        }

        selectSql.from(entityTableName)

        def entityIdentifier = identifier(entityNode)

        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)
            IdentifierPropertyModel associatedIdentifier = identifier(ap.associatedType)

            selectSql.leftOuterJoin(ap.joinTable, ap.joinTable, ap.entityColumn, entityTableName, entityIdentifier.columnName)
            selectSql.leftOuterJoin(associatedTable, ap.joinTable, ap.assocColumn, associatedTable, associatedIdentifier.columnName)
        }

        components(entityNode).each { ap ->
            selectSql.leftOuterJoin(ap.lookupTable, ap.lookupTable, ap.entityColumn, entityTableName, entityIdentifier.columnName)
        }

        selectSql.wheres(whereCriteria)

        if (orders) {
            selectSql.order(orders)
        }

        String sql = selectSql.build()

        logSql entityNode, sql

        sql
    }

    private static void addColumns(SelectSql selectSql, EntityPropertyModel p, String table, String prefix) {
        if (p instanceof EmbeddedPropertyModel) {
            p.columnNames.each { cn ->
                selectSql.column(table, cn, "${prefix}_$cn")
            }
        } else {
            selectSql.column(table, p.columnName as String, "${prefix}_${p.columnName}")
        }
    }

    private static void logSql(ClassNode entityNode, String string) {
        trace RetrievalSql, SEPARATOR
        trace RetrievalSql, 'Sql for entity ({}): {}', entityNode.name, string
        trace RetrievalSql, SEPARATOR
    }
}
