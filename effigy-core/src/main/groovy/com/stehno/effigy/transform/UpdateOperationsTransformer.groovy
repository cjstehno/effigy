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
import com.stehno.effigy.transform.model.AssociationPropertyModel
import com.stehno.effigy.transform.model.ComponentPropertyModel
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.VersionerPropertyModel
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

import static com.stehno.effigy.logging.Logger.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.SqlBuilder.update
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.code
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Injects the Update CRUD operations into an Entity repository.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString')
class UpdateOperationsTransformer implements ASTTransformation {

    private static final String NEWLINE = '\n'
    private static final String COMMA = ','
    private static final String ID = 'id'
    private static final String ENTITY = 'entity'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')
            info UpdateOperationsTransformer, 'Adding update operations to repository ({})', repositoryNode.name

            injectUpdateMethod repositoryNode, entityNode

        } else {
            warn UpdateOperationsTransformer, 'UpdateOperations can only be applied to classes annotated with @EffigyRepository - ignored.'
        }
    }

    // FIXME: this depends on teh create method being injected - need to decouple the saveENTITY calls so that update/create can exist separately

    private static void injectUpdateMethod(final ClassNode repositoryClassNode, ClassNode entityNode) {
        info UpdateOperationsTransformer, 'Injecting update method into repository for {}', entityNode.name
        try {
            components(entityNode).each { ap ->
                injectComponentUpdateMethod repositoryClassNode, entityNode, ap
            }

            def vars = []

            entityProperties(entityNode, false).each { p ->
                if (p instanceof EmbeddedPropertyModel) {
                    p.fieldNames.each { pf ->
                        vars << "entity.${p.propertyName}?.$pf"
                    }
                } else {
                    vars << "entity.${p.propertyName}"
                }
            }

            def nodes = code('''
                <% if(versioner){ %>
                def currentVersion = entity.${versioner.propertyName} ?: 0
                entity.${versioner.propertyName} = currentVersion + 1
                <% } %>

                jdbcTemplate.update(
                    '$sql',
                    ${vars.join(',')},
                    entity.${identifier.propertyName}
                    <% if(versioner){ %>
                        ,currentVersion
                    <% } %>
                )

                $o2m
                $o2o
            ''',
                vars: vars,
                sql: sql(entityNode),
                table: entityTable(entityNode),
                versioner: versioner(entityNode),
                identifier: identifier(entityNode),
                o2m: associations(entityNode).collect { AssociationPropertyModel o2m ->
                    "save${o2m.propertyName.capitalize()}(entity)"
                }.join(NEWLINE),
                o2o: components(entityNode).collect { ComponentPropertyModel ap ->
                    "update${ap.propertyName.capitalize()}(entity.${identifier(entityNode).propertyName}, entity.${ap.propertyName})"
                }.join(NEWLINE)
            )

            repositoryClassNode.addMethod(new MethodNode(
                'update',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                [new Parameter(newClass(entityNode), ENTITY)] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            error UpdateOperationsTransformer, 'Unable to inject update methods for entity ({}): {}', entityNode.name, ex.message
            throw ex
        }
    }

    private static void injectComponentUpdateMethod(ClassNode repositoryNode, ClassNode entityNode, ComponentPropertyModel o2op) {
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

        repositoryNode.addMethod(new MethodNode(
            "update${o2op.propertyName.capitalize()}",
            Modifier.PROTECTED,
            ClassHelper.VOID_TYPE,
            [param(OBJECT_TYPE, ID), param(newClass(o2op.type), ENTITY)] as Parameter[],
            null,
            statement
        ))
    }

    private static String sql(ClassNode entityNode) {
        def setters = []
        entityProperties(entityNode, false).each { p ->
            if (p instanceof EmbeddedPropertyModel) {
                p.columnNames.each { cn ->
                    setters << "$cn = ?"
                }
            } else {
                setters << "${p.columnName}=?"
            }
        }

        def wheres = ["${identifier(entityNode).columnName} = ?"]

        VersionerPropertyModel versionProperty = versioner(entityNode)
        if (versionProperty) {
            wheres << "${versionProperty.columnName} = ?"
        }

        update().table(entityTable(entityNode)).sets(setters).wheres(wheres).build()
    }
}
