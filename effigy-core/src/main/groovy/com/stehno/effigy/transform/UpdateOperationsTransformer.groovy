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
import static com.stehno.effigy.logging.Logger.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.code
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.annotation.EffigyRepository
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.OneToManyPropertyModel
import com.stehno.effigy.transform.model.OneToOnePropertyModel
import com.stehno.effigy.transform.model.VersionerPropertyModel
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier
/**
 * Injects the Update CRUD operations into an Entity repository.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class UpdateOperationsTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(EffigyRepository))[0]
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
            oneToOneAssociations(entityNode).each { ap ->
                injectO2OUpdateMethod repositoryClassNode, entityNode, ap
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
                o2m: oneToManyAssociations(entityNode).collect { OneToManyPropertyModel o2m ->
                    "save${o2m.propertyName.capitalize()}(entity)"
                }.join('\n'),
                o2o: oneToOneAssociations(entityNode).collect { OneToOnePropertyModel ap ->
                    "update${ap.propertyName.capitalize()}(entity.${identifier(entityNode).propertyName}, entity.${ap.propertyName})"
                }.join('\n')
            )

            repositoryClassNode.addMethod(new MethodNode(
                'update',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                [new Parameter(newClass(entityNode), 'entity')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            error UpdateOperationsTransformer, 'Unable to inject update methods for entity ({}): {}', entityNode.name, ex.message
            throw ex
        }
    }

    private static void injectO2OUpdateMethod(ClassNode repositoryNode, ClassNode entityNode, OneToOnePropertyModel o2op) {
        def statement = codeS(
            '''
                if( !id || !entity ){
                    jdbcTemplate.update('delete from $assocTable where $idCol = ?', id)
                } else {
                    int count = jdbcTemplate.update( 'update $assocTable set $updates where $idCol = ?', id )

                    if( count != 1 ){
                        throw new RuntimeException('Update count for $name (' + count + ') did not match expected count (1) - update failed.')
                    }
                }
            ''',
            name: o2op.propertyName,
            idCol: o2op.identifierColumn,
            assocTable: o2op.table,
            updates: oneToOneAssociations(entityNode).collect { ap ->
                entityProperties(ap.type).collect { p ->
                    "${p.columnName} = entity.${p.propertyName}"
                }.join(',')
            }.join(',')
        )

        repositoryNode.addMethod(new MethodNode(
            "update${o2op.propertyName.capitalize()}",
            Modifier.PROTECTED,
            ClassHelper.VOID_TYPE,
            [param(OBJECT_TYPE, 'id'), param(newClass(entityNode), 'entity')] as Parameter[],
            null,
            statement
        ))
    }

    private static String sql(ClassNode entityNode) {
        String table = entityTable(entityNode)

        def columns = []
        entityProperties(entityNode, false).each { p ->
            if (p instanceof EmbeddedPropertyModel) {
                p.columnNames.each { cn ->
                    columns << "$cn = ?"
                }
            } else {
                columns << "${p.columnName}=?"
            }
        }

        String columnUpdates = columns.join(',')

        String identifier = identifier(entityNode).columnName

        VersionerPropertyModel versionProperty = versioner(entityNode)
        String versionCriteria = versionProperty ? "and ${versionProperty.columnName} = ?" : ''

        "update $table set $columnUpdates where $identifier = ? $versionCriteria"
    }
}
