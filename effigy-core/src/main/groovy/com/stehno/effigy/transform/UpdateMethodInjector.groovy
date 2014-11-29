package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AstUtils.code

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
                [new Parameter(model.type, 'entity')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
