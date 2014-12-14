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
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.make

import com.stehno.effigy.annotation.Repository
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 12/6/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString')
class DeleteOperationsTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')
            info DeleteOperationsTransformer, 'Adding delete operations to repository ({})', repositoryNode.name

            injectDeleteMethod repositoryNode, entityNode
            injectDeleteAllMethod repositoryNode, entityNode

        } else {
            warn DeleteOperationsTransformer, 'DeleteOperations can only be applied to classes annotated with @EffigyRepository - ignored.'
        }
    }

    private static void injectDeleteMethod(final ClassNode repositoryNode, ClassNode entityNode) {
        info DeleteOperationsTransformer, 'Injecting delete method into repository ({}).', repositoryNode.name

        try {
            repositoryNode.addMethod(new MethodNode(
                'delete',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [new Parameter(identifier(entityNode).type, 'entityId')] as Parameter[],
                null,
                codeS(
                    '''
                    <%
                    if(hasAssoc){
                        oneToManys.each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.joinTable} where ${ap.joinTable}.${ap.entityColumn}=?', entityId)
                    <%  }
                        oneToOnes.each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.lookupTable} where ${ap.lookupTable}.${ap.entityColumn}=?', entityId)
                    <%  }
                    } %>

                    return( jdbcTemplate.update(
                        'delete from ${table} where ${identifier.columnName}=?',
                        entityId
                    ) == 1)
                    ''',
                    table: entityTable(entityNode),
                    identifier: identifier(entityNode),
                    hasAssoc: hasAssociatedEntities(entityNode),
                    oneToManys: associations(entityNode),
                    oneToOnes: components(entityNode)
                )
            ))
        } catch (ex) {
            error DeleteOperationsTransformer, 'Problem injecting delete method into repository ({}): {}', repositoryNode.name, ex.message
            throw ex
        }
    }

    private static void injectDeleteAllMethod(final ClassNode repositoryNode, ClassNode entityNode) {
        info DeleteOperationsTransformer, 'Injecting deleteAll method into repository for {}', entityNode.name

        try {
            repositoryNode.addMethod(new MethodNode(
                'deleteAll',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [] as Parameter[],
                null,
                codeS(
                    '''
                    <%
                    if(hasAssoc){
                        oneToManys.each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.joinTable}')
                    <%  }
                        oneToOnes.each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.lookupTable}')
                    <%  }
                    } %>

                    return( jdbcTemplate.update('delete from ${table}') >= 1 )
                    ''',
                    table: entityTable(entityNode),
                    hasAssoc: hasAssociatedEntities(entityNode),
                    oneToManys: associations(entityNode),
                    oneToOnes: components(entityNode)
                )
            ))
        } catch (ex) {
            error DeleteOperationsTransformer, 'Problem injecting deleteAll method into repository ({}): {}', repositoryNode.name, ex.message
            throw ex
        }
    }
}
