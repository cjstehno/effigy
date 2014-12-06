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

import static com.stehno.effigy.transform.util.AstUtils.codeS

import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.model.EntityModel
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/27/2014.
 */
class DeleteMethodInjector {

    static void injectDeleteMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        Logger.info DeleteMethodInjector, 'Injecting delete method into repository for {}', model.type.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'delete',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [new Parameter(model.identifier.type, 'entityId')] as Parameter[],
                null,
                codeS(
                    '''
                    <%
                    if(model.hasAssociations()){
                        model.findAssociationProperties().each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.table} where ${ap.table}.${ap.entityId}=?', entityId)
                    <%  }
                    } %>

                    return( jdbcTemplate.update(
                        'delete from ${model.table} where ${model.identifier.columnName}=?',
                        entityId
                    ) == 1)
                    ''',
                    model: model
                )
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }

    static void injectDeleteAllMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        Logger.info DeleteMethodInjector, 'Injecting deleteAll method into repository for {}', model.type.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'deleteAll',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [] as Parameter[],
                null,
                codeS(
                    '''
                    <%
                    if(model.hasAssociations()){
                        model.findAssociationProperties().each { ap-> %>
                            jdbcTemplate.update('delete from ${ap.table}')
                    <%  }
                    } %>

                    return( jdbcTemplate.update('delete from ${model.table}') >= 1 )
                    ''',
                    model: model
                )
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
