package com.stehno.effigy.transform

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/27/2014.
 */
class RetrieveMethodInjector {

    static void injectRetrieveMethod(final ClassNode repositoryClassNode, final EntityInfo entityInfo) {
        FieldNode mapperNode = entityInfo.type.fields.find { f -> f.static && f.name == 'ROW_MAPPER' }

        Statement statement = new ReturnStatement(new MethodCallExpression(
            new VariableExpression('jdbcTemplate'),
            'queryForObject',
            new ArgumentListExpression([
                new ConstantExpression("select ${entityInfo.fieldNamesString(true)} from ${entityInfo.table} where ${entityInfo.idFieldName}=?" as String),
                new FieldExpression(mapperNode),
                new VariableExpression('entityId', entityInfo.idType)
            ])
        ))

        repositoryClassNode.addMethod(new MethodNode(
            'retrieve',
            Modifier.PUBLIC,
            entityInfo.type,
            [new Parameter(entityInfo.idType, 'entityId')] as Parameter[],
            null,
            statement
        ))
    }
}
