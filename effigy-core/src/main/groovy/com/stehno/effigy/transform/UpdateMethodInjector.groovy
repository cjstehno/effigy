package com.stehno.effigy.transform

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase

import java.lang.reflect.Modifier
/**
 * Created by cjstehno on 11/28/2014.
 */
class UpdateMethodInjector {

    static void injectUpdateMethod(final ClassNode repositoryClassNode, final EntityModel entityInfo) {
        try {
            def columnUpdates = []
            def vars = []

            entityInfo.findProperties(false).each { p->
                columnUpdates << "${p.columnName}=?"
                vars << "entity.${p.propertyName}"
            }

            def nodes = new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, true, """
                jdbcTemplate.update(
                    'update people set ${columnUpdates.join(',')} where ${entityInfo.identifier.columnName}=?',
                    ${vars.join(',')},
                    entity.${entityInfo.identifier.propertyName}
                )
            """)

            repositoryClassNode.addMethod(new MethodNode(
                'update',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                [new Parameter(entityInfo.type, 'entity')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
