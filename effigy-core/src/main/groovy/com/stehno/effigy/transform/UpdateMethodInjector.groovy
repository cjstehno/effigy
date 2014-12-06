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

import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.code
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.model.OneToManyPropertyModel
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

    // FIXME: this depends on teh create method being injected - need to decouple the saveENTITY calls so that update/create can exist separately

    static void injectUpdateMethod(final ClassNode repositoryClassNode, ClassNode entityNode) {
        Logger.info UpdateMethodInjector, 'Injecting update method into repository for {}', entityNode.name
        try {
            def columnUpdates = []
            def vars = []

            entityProperties(entityNode, false).each { p ->
                columnUpdates << "${p.columnName}=?"
                vars << "entity.${p.propertyName}"
            }

            def nodes = code('''
                <% if(versioner){ %>
                def currentVersion = entity.${versioner.propertyName} ?: 0
                entity.${versioner.propertyName} = currentVersion + 1
                <% } %>

                jdbcTemplate.update(
                    'update people set ${columnUpdates.join(',')} where ${identifier.columnName}=? <% if(versioner){ %>and ${versioner.columnName}=?<% } %>',
                    ${vars.join(',')},
                    entity.${identifier.propertyName}
                    <% if(versioner){ %>
                        ,currentVersion
                    <% } %>
                )

                $o2m
            ''',
                vars: vars,
                versioner: versioner(entityNode),
                identifier: identifier(entityNode),
                columnUpdates: columnUpdates,
                o2m: oneToManyAssociations(entityNode).collect { OneToManyPropertyModel o2m ->
                    "save${o2m.propertyName.capitalize()}(entity)"
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
            ex.printStackTrace()
        }
    }
}
