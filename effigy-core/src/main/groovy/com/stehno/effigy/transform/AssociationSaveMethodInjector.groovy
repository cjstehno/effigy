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
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.model.EntityModel.associations
import static com.stehno.effigy.transform.model.EntityModel.identifier
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static java.lang.reflect.Modifier.PROTECTED
import static org.codehaus.groovy.ast.ClassHelper.VOID_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Created by cjstehno on 12/28/14.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class AssociationSaveMethodInjector implements ASTTransformation {

    private static final String ENTITY = 'entity'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        MethodNode methodNode = nodes[1] as MethodNode
        ClassNode repoNode = methodNode.declaringClass

        AnnotationNode repositoryAnnot = repoNode.getAnnotations(make(Repository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')

            associations(entityNode).each { AssociationPropertyModel ap ->
                injectAssociationSaveMethod repoNode, entityNode, ap
            }

        } else {
            error getClass(), 'Repository method annotations may only be applied to methods of an Effigy Repository class.'
            throw new EffigyTransformationException()
        }
    }

    @SuppressWarnings('GStringExpressionWithinString')
    private static void injectAssociationSaveMethod(ClassNode repositoryNode, ClassNode entityNode, AssociationPropertyModel assoc) {
        def methodName = "save${assoc.propertyName.capitalize()}"
        def methodParams = [param(newClass(entityNode), ENTITY)] as Parameter[]

        if (!repositoryNode.hasMethod(methodName, methodParams)) {
            info AssociationSaveMethodInjector, 'Injecting association ({}) save method for entity ({})', assoc.propertyName, entityNode
            try {
                def statement = codeS(
                    '''
                        int expects = 0
                        if( entity.${name} instanceof Collection ){
                            expects = entity.${name}?.size() ?: 0
                        } else {
                            expects = entity.${name} != null ? 1 : 0
                        }

                        int count = 0
                        def ent = entity

                        jdbcTemplate.update('delete from $assocTable where $tableEntIdName=?', ent.${entityIdName})

                        entity.${name}.each { itm->
                            count += jdbcTemplate.update(
                                'insert into $assocTable ($tableEntIdName,$tableAssocIdName) values (?,?)',
                                ent.${entityIdName},
                                itm.${assocIdName}
                            )
                        }

                        if( count != expects ){
                            entity.${entityIdName} = 0
                            throw new RuntimeException(
                                'Insert count for $name (' + count + ') did not match expected count (' + expects + ') - save failed.'
                            )
                        }
                    ''',
                    name: assoc.propertyName,
                    assocTable: assoc.joinTable,
                    tableEntIdName: assoc.entityColumn,
                    tableAssocIdName: assoc.assocColumn,
                    entityIdName: identifier(entityNode).propertyName,
                    assocIdName: identifier(assoc.associatedType).propertyName
                )

                repositoryNode.addMethod(methodN(PROTECTED, methodName, VOID_TYPE, statement, methodParams))

            } catch (ex) {
                error AssociationSaveMethodInjector, 'Unable to inject association save method for entity ({}): {}', entityNode.name, ex.message
                throw ex
            }
        }
    }
}
