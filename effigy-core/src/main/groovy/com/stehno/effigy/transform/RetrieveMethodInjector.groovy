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
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.AstUtils.codeS
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

import java.lang.reflect.Modifier
/**
 * Code generators for the entity repository retrieval methods provided by the CrudOperations interface.
 */
class RetrieveMethodInjector {

    /**
     * Injects the retrieve method into an entity repository class. This method is an implementation of the
     * CrudOperations.retrieve(E entityId) method.
     *
     * @param repositoryClassNode
     * @param model
     */
    static void injectRetrieveMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        info RetrieveMethodInjector, 'Injecting retrieve method into repository for {}', model.type.name

        try {

            Statement statement = block(
                declS(varX('mapper'), callX(classX(newClass(model.type)), 'rowMapper')),

                codeS(
                    '''
                    jdbcTemplate.queryForObject(
                        'select ${model.columnNames().join(',')} from ${model.table} where ${model.identifier.columnName}=?',
                        mapper,
                        entityId
                    )
                    ''',
                    model: model
                )

            )

//            Statement statement = new ReturnStatement(new MethodCallExpression(
//                new VariableExpression('jdbcTemplate'),
//                'queryForObject',
//                new ArgumentListExpression([
//                    new ConstantExpression("select ${model.columnNames().join(',')} from ${model.table} where ${model.identifier.columnName}=?" as String),
//                    callX(classX(newClass(model.type)),'rowMapper'),
//                    new VariableExpression('entityId', model.identifier.type)
//                ])
//            ))

            /*
            FIXME: add relations for O2M

            need to bring in relations as join rather than separate sql call for each relation

         */

            repositoryClassNode.addMethod(new MethodNode(
                'retrieve',
                Modifier.PUBLIC,
                newClass(model.type),
                [new Parameter(model.identifier.type, 'entityId')] as Parameter[],
                null,
                statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }

    static void injectRetrieveAllMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        info RetrieveMethodInjector, 'Injecting retrieve All method into repository for {}', model.type.name
        try {
            def colNames = model.columnNames().join(',')

            Statement statement = new ReturnStatement(new MethodCallExpression(
                new VariableExpression('jdbcTemplate'),
                'query',
                new ArgumentListExpression([
                    new ConstantExpression("select $colNames from ${model.table}" as String),
                    callX(classX(newClass(model.type)), 'rowMapper'),
                ])
            ))

            repositoryClassNode.addMethod(new MethodNode(
                'retrieveAll',
                Modifier.PUBLIC,
                makeClassSafe(List),
                [] as Parameter[],
                null,
                statement
            ))

        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
