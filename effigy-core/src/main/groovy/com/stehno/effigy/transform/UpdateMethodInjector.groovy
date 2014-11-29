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

import static com.stehno.effigy.transform.AstUtils.code
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.logging.Logger
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/28/2014.
 */
class UpdateMethodInjector {

    static void injectUpdateMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        Logger.info UpdateMethodInjector, 'Injecting update method into repository for {}', model.type.name
        try {
            def columnUpdates = []
            def vars = []

            model.findProperties(false).each { p ->
                columnUpdates << "${p.columnName}=?"
                vars << "entity.${p.propertyName}"
            }

            def nodes = code('''
                <% if(model.versioner){ %>
                def currentVersion = entity.${model.versioner.propertyName} ?: 0
                entity.${model.versioner.propertyName} = currentVersion + 1
                <% } %>

                jdbcTemplate.update(
                    'update people set ${columnUpdates.join(',')} where ${model.identifier.columnName}=? <% if(model.versioner){ %>and ${model.versioner.columnName}=?<% } %>',
                    ${vars.join(',')},
                    entity.${model.identifier.propertyName}
                    <% if(model.versioner){ %>
                        ,currentVersion
                    <% } %>
                )

                $o2m
            ''',
                model: model,
                vars: vars,
                columnUpdates: columnUpdates,
                o2m: model.findPropertiesByType(OneToManyPropertyModel).collect { OneToManyPropertyModel o2m ->
                    "save${o2m.propertyName.capitalize()}(entity)"
                }.join('\n')
            )

            repositoryClassNode.addMethod(new MethodNode(
                'update',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                [new Parameter(newClass(model.type), 'entity')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
