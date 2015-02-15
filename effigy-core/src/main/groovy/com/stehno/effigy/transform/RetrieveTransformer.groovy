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

package com.stehno.effigy.transform

import com.stehno.effigy.annotation.Limit
import com.stehno.effigy.annotation.Offset
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.EntityPropertyModel
import com.stehno.effigy.transform.model.IdentifierPropertyModel
import com.stehno.effigy.transform.sql.SelectSqlBuilder
import com.stehno.effigy.transform.sql.SqlTemplate
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement

import static SelectSqlBuilder.select
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractInteger
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

/**
 * Transformer used to process the @Retrieve annotations.
 */
class RetrieveTransformer extends MethodImplementingTransformation {

    private static final String RESULTS = 'results'
    private static final String PLACEHOLDER = '?'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType == OBJECT_TYPE || returnType == entityNode || returnType.implementsInterface(makeClassSafe(Collection))
    }

    @Override @SuppressWarnings('GroovyAssignabilityCheck')
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        if (hasAssociatedEntities(entityNode)) {
            code.addStatement generateSelectWithAssociations(entityNode, annotationNode, methodNode)
        } else {
            code.addStatement generateSelectWithoutAssociations(entityNode, annotationNode, methodNode)
        }

        if (methodNode.returnType == entityNode) {
            code.addStatement returnS(callX(varX(RESULTS), 'getAt', constX(0)))
        } else {
            code.addStatement returnS(varX(RESULTS))
        }

        updateMethod repoNode, methodNode, code
    }

    private static Statement generateSelectWithoutAssociations(ClassNode entityNode, AnnotationNode annotationNode, MethodNode methodNode) {
        SelectSqlBuilder sql = select().columns(listColumnNames(entityNode)).from(entityTable(entityNode))

        applyParameters(sql, new AnnotatedMethod(annotationNode, entityNode, methodNode))
        applyOrders(sql, annotationNode, entityNode)
        applyPagination(sql, annotationNode, methodNode, Offset)
        applyPagination(sql, annotationNode, methodNode, Limit)

        declS(varX(RESULTS), queryX(
            sql.build(),
            entityRowMapper(entityNode),
            sql.params
        ))
    }

    private static Statement generateSelectWithAssociations(ClassNode entityNode, AnnotationNode annotationNode, MethodNode methodNode) {
        SelectSqlBuilder sql = select()

        String entityTableName = entityTable(entityNode)

        // add entity columns
        entityProperties(entityNode).each { p ->
            addColumns(sql, p, entityTableName, entityTableName)
        }

        // add association cols
        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)

            entityProperties(ap.associatedType).each { p ->
                addColumns(sql, p, associatedTable, ap.propertyName)
            }
        }

        // add component cols
        components(entityNode).each { ap ->
            entityProperties(ap.type).each { p ->
                sql.column(ap.lookupTable, p.columnName as String, "${ap.propertyName}_${p.columnName}")
            }
        }

        sql.from(entityTableName)

        def entityIdentifier = identifier(entityNode)

        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)
            IdentifierPropertyModel associatedIdentifier = identifier(ap.associatedType)

            sql.leftOuterJoin(ap.joinTable, ap.joinTable, ap.entityColumn, entityTableName, entityIdentifier.columnName)
            sql.leftOuterJoin(associatedTable, ap.joinTable, ap.assocColumn, associatedTable, associatedIdentifier.columnName)
        }

        components(entityNode).each { ap ->
            sql.leftOuterJoin(ap.lookupTable, ap.lookupTable, ap.entityColumn, entityTableName, entityIdentifier.columnName)
        }

        applyParameters(sql, new AnnotatedMethod(annotationNode, entityNode, methodNode))
        applyOrders(sql, annotationNode, entityNode)

        declS(varX(RESULTS), queryX(
            sql.build(),
            entityCollectionExtractor(
                entityNode,
                extractPagination(annotationNode, methodNode, Offset),
                extractPagination(annotationNode, methodNode, Limit)
            ),
            sql.params
        ))
    }

    private static void addColumns(SelectSqlBuilder selectSql, EntityPropertyModel p, String table, String prefix) {
        if (p instanceof EmbeddedPropertyModel) {
            p.columnNames.each { cn ->
                selectSql.column(table, cn, "${prefix}_$cn")
            }
        } else {
            selectSql.column(table, p.columnName as String, "${prefix}_${p.columnName}")
        }
    }

    // limit and offset - a bit of a hack
    private static Expression extractPagination(AnnotationNode annotationNode, MethodNode methodNode, Class annoClass) {
        def annotatedParam = findAnnotatedIntParam(methodNode, annoClass)

        Integer value = extractInteger(annotationNode, annoClass.simpleName.toLowerCase())
        if (value > -1) {
            return constX(value)

        } else if (annotatedParam) {
            return varX(annotatedParam.name)
        }

        return null
    }

    // limit and offset - a bit of a hack
    private static void applyPagination(SelectSqlBuilder sql, AnnotationNode annotationNode, MethodNode methodNode, Class annoClass) {
        def param = findAnnotatedIntParam(methodNode, annoClass)
        Integer value = extractInteger(annotationNode, annoClass.simpleName.toLowerCase())

        if (value > -1) {
            sql.offset(PLACEHOLDER, constX(value))

        } else if (param) {
            sql.offset(PLACEHOLDER, varX(param.name))
        }
    }

    private static Parameter findAnnotatedIntParam(MethodNode methodNode, Class annoClass) {
        methodNode.parameters.find { p -> p.getAnnotations(make(annoClass)) && p.type == int_TYPE }
    }

    private static void applyOrders(SelectSqlBuilder sql, AnnotationNode annotationNode, ClassNode entityNode) {
        String orderTemplate = extractString(annotationNode, 'order')
        if (orderTemplate) {
            sql.order(new SqlTemplate(orderTemplate).sql(entityNode))
        } /*else {
            FIXME: support for runtime order param
        }*/
    }
}
