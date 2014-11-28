package com.stehno.effigy.transform
import static com.stehno.effigy.transform.AstUtils.*
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import java.lang.reflect.Modifier
/**
 * Injects the code for the repository entity create method.
 */
class CreateMethodInjector {

    static void injectCreateMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        try {
            def statement = block(
                declS(varX('keys'), ctorX(make(GeneratedKeyHolder))),

                model.versioner ? codeS('entity.$name = 0', name: model.versioner.propertyName) : new EmptyStatement(),

                declS(varX('factory'), ctorX(make(PreparedStatementCreatorFactory), args(
                    constX("insert into ${model.table} (${model.findProperties(false).collect { it.columnName }.join(',')}) values (${model.findProperties(false).collect { '?' }.join(',')})" as String),
                    arrayX(ClassHelper.int_TYPE, model.findSqlTypes(false).collect { typ ->
                        constX(typ)
                    })
                ))),

                declS(varX('paramValues'), arrayX(OBJECT_TYPE, model.findProperties(false).collect { pi ->
                    if (pi.enumeration) {
                        codeX('entity.${name}?.name()', name: pi.propertyName)
                    } else {
                        propX(varX('entity'), constX(pi.propertyName))
                    }
                })),

                declS(varX('psc'), callX(varX('factory'), 'newPreparedStatementCreator', args(varX('paramValues')))),

                stmt(callX(varX('jdbcTemplate'), 'update', args(varX('psc'), varX('keys')))),

                codeS('entity.${name} = keys.key', name:model.identifier.propertyName),
                codeS('return keys.key')
            )

            repositoryClassNode.addMethod(new MethodNode(
                'create',
                Modifier.PUBLIC,
                model.identifier.type,
                [new Parameter(model.type, 'entity')] as Parameter[],
                null,
                statement
            ))

        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
