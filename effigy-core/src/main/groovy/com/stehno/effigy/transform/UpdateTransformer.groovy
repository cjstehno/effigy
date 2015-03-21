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

import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.model.AssociationPropertyModel
import com.stehno.effigy.transform.model.ComponentPropertyModel
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.sql.UpdateSqlBuilder
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.Statement

import static UpdateSqlBuilder.update
import static com.stehno.effigy.transform.CreateTransformer.resolveEntityVariable
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.updateX
import static java.lang.reflect.Modifier.PROTECTED
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass
import static org.codehaus.groovy.syntax.Token.newSymbol
import static org.codehaus.groovy.syntax.Types.MINUS

/**
 * Transformer used to process the <code>@Update</code> annotations.
 */
class UpdateTransformer extends MethodImplementingTransformation {

    private static final Logger log = Logger.factory(UpdateTransformer)

    // FIXME: pull out common constants
    private static final String ENTITY = 'entity'
    private static final String COMMA = ','
    private static final String ID = 'id'
    private static final String NEWLINE = '\n'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType in [VOID_TYPE, int_TYPE, Integer_TYPE, boolean_TYPE, Boolean_TYPE]
    }

    @Override
    @SuppressWarnings(['GroovyAssignabilityCheck', 'GStringExpressionWithinString'])
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        ensureParameters methodNode

        def (String entityVar, Statement entityCreator) = resolveEntityVariable(entityNode, methodNode)

        components(entityNode).each { ap ->
            injectComponentUpdateMethod repoNode, entityNode, ap
        }

        def sql = update().table(entityTable(entityNode))
        applyParameters(sql, new AnnotatedMethod(annotationNode, entityNode, methodNode), true)
        applySetters(sql, entityNode, entityVar)

        def entityIdent = identifier(entityNode)
        def versioner = versioner(entityNode)

        // if no sql template is defined, we want default behavior
        if (!sql.wheres) {
            sql.where("${entityIdent.columnName} = ?", propX(varX(entityVar), entityIdent.propertyName))

            if (versioner) {
                // minus one since we need the old version in the where clause
                sql.where(
                    "${versioner.columnName} = ?",
                    new BinaryExpression(propX(varX(entityVar), versioner.propertyName), newSymbol(MINUS, -1, -1), constX(1))
                )
            }
        }

        def code = block()

        if (entityCreator) {
            code.addStatement(entityCreator)
        }

        if (versioner) {
            code.addStatement(codeS(
                '${entity}.${versioner.propertyName} = ${entity}.${versioner.propertyName} + 1',
                entity: entityVar,
                versioner: versioner
            ))
        }

        code.addStatement(declS(varX('count'), updateX(sql.build(), sql.params)))

        // FIXME: should throw more explicit exception
        code.addStatement(codeS('''
                if( count ){
                    $o2m
                    $o2o
                    return count
                } else {
                    throw new Exception('Update failed: expected at least one updated row but there were none.')
                }
            ''',
            o2m: associations(entityNode).collect { AssociationPropertyModel o2m ->
                "save${o2m.propertyName.capitalize()}(${entityVar})"
            }.join(NEWLINE),
            o2o: components(entityNode).collect { ComponentPropertyModel ap ->
                "update${ap.propertyName.capitalize()}(${entityVar}.${entityIdent.propertyName}, ${entityVar}.${ap.propertyName})"
            }.join(NEWLINE)
        ))

        updateMethod repoNode, methodNode, code

        log.debug 'Implemented repository ({}) Update method ({})', repoNode.name, methodNode.name
    }

    private static void applySetters(UpdateSqlBuilder sql, ClassNode entityNode, String entityVar) {
        entityProperties(entityNode, false).each { p ->
            if (p instanceof EmbeddedPropertyModel) {
                p.fieldNames.eachWithIndex { pf, idx ->
                    sql.set("${p.columnNames[idx]}=?", new PropertyExpression(propX(varX(entityVar), p.propertyName), constX(pf), true))
                }
            } else {
                sql.set("${p.columnName}=?", propX(varX(entityVar), p.propertyName))
            }
        }
    }

    private static void injectComponentUpdateMethod(ClassNode repositoryNode, ClassNode entityNode, ComponentPropertyModel o2op) {
        def methodName = "update${o2op.propertyName.capitalize()}"
        def methodParams = params(param(OBJECT_TYPE, ID), param(newClass(o2op.type), ENTITY))

        if (!repositoryNode.hasMethod(methodName, methodParams)) {
            def colUpdates = []
            def varUpdates = []

            components(entityNode).each { ap ->
                entityProperties(ap.type).collect { p ->
                    colUpdates << "${p.columnName} = ?"
                    varUpdates << "entity.${p.propertyName}"
                }.join(COMMA)
            }

            def statement = codeS(
                '''
                if( !id || !entity ){
                    jdbcTemplate.update('delete from $assocTable where $idCol = ?', id)
                } else {
                    int count = jdbcTemplate.update( 'update $assocTable set $updates where $idCol = ?', $vars,id )

                    if( count != 1 ){
                        throw new RuntimeException('Update count for $name (' + count + ') did not match expected count (1) - update failed.')
                    }
                }
            ''',
                name: o2op.propertyName,
                idCol: o2op.entityColumn,
                assocTable: o2op.lookupTable,
                updates: colUpdates.join(COMMA),
                vars: varUpdates.join(COMMA)
            )

            repositoryNode.addMethod(methodN(PROTECTED, methodName, VOID_TYPE, statement, methodParams))
        }
    }

    private static void ensureParameters(MethodNode methodNode) {
        if (!methodNode.parameters) {
            throw new EffigyTransformationException('Update methods must accept at least an Entity or Map parameter.')
        }
    }
}
