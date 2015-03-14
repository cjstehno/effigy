/*
 * Copyright (c) 2015 Christopher J. Stehno
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

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ForStatement

import static com.stehno.effigy.logging.Logger.debug
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.DeleteSqlBuilder.delete
import static com.stehno.effigy.transform.sql.SelectSqlBuilder.select
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Transformer used to process the @Delete annotation.
 */
class DeleteTransformer extends MethodImplementingTransformation {

    private static final String ENTITY_IDS = 'entityIds'
    private static final String ENTITY_ID = 'entityId'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType in [boolean_TYPE, Boolean_TYPE, Integer_TYPE, int_TYPE]
    }

    @Override
    @SuppressWarnings('GroovyAssignabilityCheck')
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        injectAssociationDeletes entityNode, methodNode, annotationNode, code

        def sql = delete().from(entityTable(entityNode))
        applyParameters(sql, new AnnotatedMethod(annotationNode, entityNode, methodNode))

        code.addStatement(returnS(updateX(sql.build(), sql.params)))

        updateMethod repoNode, methodNode, code
    }

    private static void injectAssociationDeletes(ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode, BlockStatement code) {
        if (hasAssociatedEntities(entityNode)) {
            debug DeleteTransformer, 'Associations({}:{})', entityNode.name, methodNode.name

            injectEntityIdSelection(entityNode, methodNode, deleteNode, code)

            def closureCode = block()

            associations(entityNode).each { ap ->
                debug DeleteTransformer, '- Association({}:{}): {}', entityNode.name, methodNode.name, ap.propertyName

                def sql = delete().from(ap.joinTable).where("${ap.joinTable}.${ap.entityColumn}=?", varX(ENTITY_ID))

                closureCode.addStatement(stmt(updateX(sql.build(), sql.params)))
            }

            components(entityNode).each { ap ->
                debug DeleteTransformer, '- Component({}:{}): {}', entityNode.name, methodNode.name, ap.propertyName

                def sql = delete().from(ap.lookupTable).where("${ap.lookupTable}.${ap.entityColumn}=?", varX(ENTITY_ID))

                closureCode.addStatement(stmt(updateX(sql.build(), sql.params)))
            }

            code.addStatement(new ForStatement(param(OBJECT_TYPE, ENTITY_ID), varX(ENTITY_IDS), closureCode))
        }
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    private static void injectEntityIdSelection(ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode, BlockStatement code) {
        def ident = identifier(entityNode)

        def sql = select().column(ident.columnName).from(entityTable(entityNode))

        applyParameters(sql, new AnnotatedMethod(deleteNode, entityNode, methodNode))

        debug DeleteTransformer, 'Sql({}:Delete): {}', entityNode.name, sql

        code.addStatement(declS(varX(ENTITY_IDS), queryX(sql.build(), singleColumnRowMapper(ident.type), sql.params)))
    }
}