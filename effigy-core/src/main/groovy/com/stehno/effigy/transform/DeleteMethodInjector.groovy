package com.stehno.effigy.transform

import com.stehno.effigy.logging.Logger
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/27/2014.
 */
class DeleteMethodInjector {

    static void injectDeleteMethod(final ClassNode repositoryClassNode, final EntityModel entityInfo) {
        Logger.info DeleteMethodInjector, 'Injecting delete method into repository for {}', entityInfo.type.name

        try {
            def nodes = new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, true, """
                return( jdbcTemplate.update(
                    'delete from ${entityInfo.table} where ${entityInfo.identifier.columnName}=?',
                    entityId
                ) == 1)
            """)

            repositoryClassNode.addMethod(new MethodNode(
                'delete',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [new Parameter(entityInfo.identifier.type, 'entityId')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }

    static void injectDeleteAllMethod(final ClassNode repositoryClassNode, final EntityModel entityInfo) {
        Logger.info DeleteMethodInjector, 'Injecting deleteAll method into repository for {}', entityInfo.type.name

        try {
            def nodes = new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, true, """
                return( jdbcTemplate.update(
                    'delete from ${entityInfo.table}'
                ) >= 1)
            """)

            repositoryClassNode.addMethod(new MethodNode(
                'deleteAll',
                Modifier.PUBLIC,
                ClassHelper.boolean_TYPE,
                [] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
