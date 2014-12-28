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

import com.stehno.effigy.transform.sql.SqlTemplate
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.SqlBuilder.delete
import static com.stehno.effigy.transform.sql.SqlBuilder.select
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Transformer used to process the @Delete annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
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
        try {
            def code = block()

            injectAssociationDeletes entityNode, methodNode, annotationNode, code

            def (wheres, params) = extractParameters(entityNode, methodNode, annotationNode)

            code.addStatement(returnS(
                updateX(
                    delete().from(entityTable(entityNode)).wheres(wheres).build(),
                    params
                )
            ))

            methodNode.modifiers = Modifier.PUBLIC
            methodNode.code = code

        } catch (ex) {
            error(
                DeleteTransformer,
                'Unable to implement delete method ({}) for repository ({}): {}',
                methodNode.name,
                repoNode.name,
                ex.message
            )
            throw ex
        }
    }

    private static void injectAssociationDeletes(ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode, BlockStatement code) {
        if (hasAssociatedEntities(entityNode)) {
            injectEntityIdSelection(entityNode, methodNode, deleteNode, code)

            def closureCode = block()

            associations(entityNode).each { ap ->
                def sql = delete().from(ap.joinTable).where("${ap.joinTable}.${ap.entityColumn}=?")
                closureCode.addStatement(stmt(updateX(sql.build(), [varX(ENTITY_ID)])))
            }

            components(entityNode).each { ap ->
                def sql = delete().from(ap.lookupTable).where("${ap.lookupTable}.${ap.entityColumn}=?")
                closureCode.addStatement(stmt(updateX(sql.build(), [varX(ENTITY_ID)])))
            }

            code.addStatement(stmt(callX(varX(ENTITY_IDS), 'each', closureX(params(param(OBJECT_TYPE, ENTITY_ID)), closureCode))))
        }
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    private static void injectEntityIdSelection(ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode, BlockStatement code) {
        def (wheres, params) = extractParameters(entityNode, methodNode, deleteNode)

        code.addStatement(declS(
            varX(ENTITY_IDS),
            queryX(
                select().column(identifier(entityNode).columnName).from(entityTable(entityNode)).wheres(wheres).build(),
                singleColumnRowMapper(),
                params
            )
        ))
    }

    private static List extractParameters(ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode) {
        def wheres = []
        def params = []

        SqlTemplate template = extractSqlTemplate(deleteNode)
        if (template) {
            wheres << template.sql(entityNode)
            params.addAll(template.variableNames().collect { vn -> varX(vn[1..-1]) })

        } else {
            methodNode.parameters.each { mp ->
                wheres << "${entityProperty(entityNode, mp.name).columnName}=?"
                params << varX(mp.name)
            }
        }

        [wheres, params]
    }

    private static SqlTemplate extractSqlTemplate(final AnnotationNode node) {
        String value = extractString(node, 'value')
        value ? new SqlTemplate(value) : null
    }
}