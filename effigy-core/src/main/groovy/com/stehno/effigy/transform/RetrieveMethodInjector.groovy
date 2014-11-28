package com.stehno.effigy.transform

import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

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

    static void injectRetrieveMethod(final ClassNode repositoryClassNode, final EntityModel entityInfo) {
        FieldNode mapperNode = entityInfo.type.fields.find { f -> f.static && f.name == 'ROW_MAPPER' }

        Statement statement = new ReturnStatement(new MethodCallExpression(
            new VariableExpression('jdbcTemplate'),
            'queryForObject',
            new ArgumentListExpression([
                new ConstantExpression("select ${entityInfo.findProperties().collect { it.columnName }.join(',')} from ${entityInfo.table} where ${entityInfo.identifier.columnName}=?" as String),
                new FieldExpression(mapperNode),
                new VariableExpression('entityId', entityInfo.identifier.type)
            ])
        ))

        repositoryClassNode.addMethod(new MethodNode(
            'retrieve',
            Modifier.PUBLIC,
            entityInfo.type,
            [new Parameter(entityInfo.identifier.type, 'entityId')] as Parameter[],
            null,
            statement
        ))
    }

    static void injectRetrieveAllMethod(final ClassNode repositoryClassNode, final EntityModel entityInfo) {
        FieldNode mapperNode = entityInfo.type.fields.find { f -> f.static && f.name == 'ROW_MAPPER' }

        def colNames = entityInfo.findProperties().collect { it.columnName }.join(',')

        Statement statement = new ReturnStatement(new MethodCallExpression(
            new VariableExpression('jdbcTemplate'),
            'query',
            new ArgumentListExpression([
                new ConstantExpression("select $colNames from ${entityInfo.table}" as String),
                new FieldExpression(mapperNode)
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
    }
}
