package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AstUtils.arrayX
import static com.stehno.effigy.transform.AstUtils.codeS
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

                codeS(
                    '''
                        def paramValues = [$values] as Object[]
                        jdbcTemplate.update(factory.newPreparedStatementCreator(paramValues), keys)
                        entity.${idName} = keys.key
                        return keys.key
                    ''',

                    values: model.findProperties(false).collect { pi ->
                        if (pi.enumeration) {
                            "entity.${pi.propertyName}?.name()"
                        } else {
                            "entity.${pi.propertyName}"
                        }
                    }.join(','),

                    idName: model.identifier.propertyName
                )
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
