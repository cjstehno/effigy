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

import com.stehno.effigy.annotation.Repository
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.logging.Logger.warn
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.SqlBuilder.delete
import static com.stehno.effigy.transform.sql.SqlBuilder.select
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Transformer used to process the @Delete annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString')
class DeleteTransformer implements ASTTransformation {


    private static final String ENTITY_IDS = 'entityIds'
    private static final String ENTITY_ID = 'entityId'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode deleteNode = nodes[0] as AnnotationNode
        MethodNode methodNode = nodes[1] as MethodNode
        ClassNode repositoryNode = methodNode.declaringClass

        try {
            AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
            if (repositoryAnnot) {
                ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')

                validateReturnType methodNode.returnType
                implementDeleteMethod repositoryNode, entityNode, methodNode, deleteNode

            } else {
                warn(
                    DeleteTransformer,
                    'Delete annotation may only be applied to methods of an Effigy Repository class - ignoring.'
                )
            }

        } catch (ex) {
            error(
                DeleteTransformer,
                'Unable to implement delete method ({}) for ({}): {}',
                methodNode.name,
                repositoryNode.name,
                ex.message
            )
            throw ex
        }
    }

    private static void validateReturnType(ClassNode returnType) {
        returnType in [boolean_TYPE, Boolean_TYPE, Integer_TYPE, int_TYPE]
    }

    private static void implementDeleteMethod(ClassNode repoNode, ClassNode entityNode, MethodNode methodNode, AnnotationNode deleteNode) {
        try {
            def code = block()

            injectAssociationDeletes entityNode, methodNode, deleteNode, code

            def sql = delete().from(entityTable(entityNode))
            def params = []

            methodNode.parameters.each { p ->
                sql.where("${entityProperty(entityNode, p.name).columnName}=?")
                params << varX(p.name)
            }

            code.addStatement(returnS(updateX(sql.build(), params)))

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

    private static void injectEntityIdSelection(final ClassNode entityNode, final MethodNode methodNode, AnnotationNode deleteNode, final BlockStatement code) {
        def sql = select().column(identifier(entityNode).columnName).from(entityTable(entityNode))

        String annotSql = extractString(deleteNode, 'value' )
        if( annotSql ){
// FIXME: here
        } else {
            def qParams = []
            methodNode.parameters.each { mp ->
                sql.where("${entityProperty(entityNode, mp.name).columnName}=?")
                qParams << varX(mp.name)
            }

            code.addStatement(declS(varX(ENTITY_IDS), queryX(sql.build(), singleColumnRowMapper(), qParams)))
        }
    }
}