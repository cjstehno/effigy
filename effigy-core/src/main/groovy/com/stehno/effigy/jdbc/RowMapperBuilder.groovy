/*
 * Copyright (c) 2015 Christopher J. Stehno
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

package com.stehno.effigy.jdbc

import groovy.transform.Immutable
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.springframework.jdbc.core.RowMapper

import java.lang.annotation.*
import java.sql.ResultSet
import java.sql.SQLException

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.util.AstUtils.classN
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static com.stehno.effigy.transform.util.StringUtils.camelCaseToUnderscore
import static java.lang.reflect.Modifier.PROTECTED
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Created by cjstehno on 3/15/15.
 */
class RowMapperBuilder<T> {
    // FIXME: API documentation
    // FIXME: user guide documentation

    private final Class<? extends T> mappedType
    private final String prefix
    private final List<PropertyMapping> mappings = []

    private RowMapperBuilder(final Class<? extends T> mappedType, final String prefix) {
        this.mappedType = mappedType
        this.prefix = prefix
    }

    PropertyMapping map(String propertyName) {
        def mapping = new PropertyMapping(propertyName, prefix)
        mappings << mapping
        mapping
    }

    static <T> RowMapper<T> mapper(Class<? extends T> mappedType, String prefix = '', Closure closure) {
        def builder = new RowMapperBuilder<T>(mappedType, prefix)
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure()
        builder.build()
    }

    RowMapper<T> build() {
        new DynamicRowMapper<T>(mappedType, mappings.asImmutable())
    }
}

class PropertyMapping {

    private final String propertyName
    private String fieldName
    private Closure transformer
    private RowMapper rowMapper
    private String prefix

    PropertyMapping(String propertyName, String prefix) {
        this.propertyName = propertyName
        this.prefix = prefix
    }

    PropertyMapping from(String fieldName) {
        this.fieldName = fieldName
        this
    }

    void using(Closure transformer) {
        this.transformer = transformer
        this.rowMapper = null
    }

    void using(RowMapper rowMapper) {
        this.transformer = null
        this.rowMapper = rowMapper
    }

    // TODO: should I store desired type (or resolve it)
    void resolve(ResultSet rs, int rowNum, Object instance) {
        def value = rs.getObject("${prefix}${fieldName ?: camelCaseToUnderscore(propertyName)}")
        if (transformer) {
            instance[propertyName] = transformer.maximumNumberOfParameters == 1 ? transformer.call(value) : transformer.call()

        } else if (rowMapper) {
            instance[propertyName] = rowMapper.mapRow(rs, rowNum)

        } else {
            instance[propertyName] = value
        }

    }
}


// TODO: this wont handle immutable pogos...

@Immutable
class DynamicRowMapper<T> implements RowMapper<T> {

    Class<? extends T> mappedType
    List<PropertyMapping> mappings

    @Override
    T mapRow(ResultSet rs, int rowNum) throws SQLException {
        def instance = mappedType.newInstance()
        mappings*.resolve(rs, rowNum, instance)
        instance
    }
}

// TODO: similar to the EffigyEntityRowMapper (merge?)
abstract class CompiledRowMapper<E> implements RowMapper<E> {

    @Override
    E mapRow(ResultSet rs, int rowNum) throws SQLException {
        mappings rs, rowNum, instantiate()
    }

    abstract protected E instantiate()

    abstract protected E mappings(ResultSet rs, int rowNum, E inst)
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [CompiledMapperTransformer])
@interface CompiledMapper {
    // FIXME: needs a better name!

}

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class CompiledMapperTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        MethodNode methodNode = nodes[1]
        BlockStatement statement = methodNode.code

        MethodCallExpression methodCallEx = ((statement.statements[0] as ExpressionStatement).expression as MethodCallExpression)
        String mapperMethodName = methodCallEx.methodAsString
        // 'mapper'
        ArgumentListExpression methodArgs = methodCallEx.arguments
        // type, prefix, closure

        ClassExpression mappedTypeX
        ConstantExpression prefixX
        ClosureExpression dslClosureX

        if (methodArgs.expressions.size() == 2) {
            mappedTypeX = methodArgs.expressions[0]
            dslClosureX = methodArgs.expressions[1]

        } else {
            // 2
            mappedTypeX = methodArgs.expressions[0]
            prefixX = methodArgs.expressions[1]
            dslClosureX = methodArgs.expressions[2]
        }

        ClassNode mapperNode = buildRowMapper(methodNode.declaringClass, mappedTypeX.type, prefixX?.text ?: '', dslClosureX.code.statements, source)
        methodNode.code = returnS(ctorX(mapperNode))
    }

    private
    static ClassNode buildRowMapper(ClassNode owningClass, ClassNode entityNode, String prefix, List<Statement> dslStatements, SourceUnit source) {
        String mapperName = "${owningClass.packageName}.${entityNode.nameWithoutPackage}RowMapper"
        try {
            ClassNode mapperClassNode = classN(mapperName, CompiledRowMapper)

            mapperClassNode.addMethod(methodN(PROTECTED, 'instantiate', newClass(entityNode), returnS(ctorX(newClass(entityNode)))))

            // standard mapping
            BlockStatement mappingsSt = block()

            dslStatements.each { ExpressionStatement s ->
                MethodCallExpression mcx = s.expression
                String propertyName = mcx.arguments.expressions[0].text

                String configFieldName = null
                // TODO : support 'from' method calls
                String fieldName = "${prefix}${configFieldName ?: camelCaseToUnderscore(propertyName)}"

                // mapper/trasformer
                mappingsSt.addStatement(stmt(
                    callX(varX('entity'), "set${propertyName.capitalize()}", args(callX(varX('rs'), 'getObject', args(constX(fieldName)))))
                ))
            }

            mappingsSt.addStatement(returnS(varX('entity')))

            /*
            rowmapper
            entity.propertyName = otherMapper.mapRow(rs,rowNum)

            transformer
            entity.propertyName = transformer(inlined).call()
             */

            mapperClassNode.addMethod(methodN(
                PROTECTED,
                'mappings',
                newClass(entityNode),
                mappingsSt,
                [param(make(ResultSet), 'rs'), param(int_TYPE, 'rowNum'), param(OBJECT_TYPE, 'entity')] as Parameter[]
            ))

            source.AST.addClass(mapperClassNode)

            info getClass(), 'Injected row mapper ({}) for {}', mapperClassNode.name, entityNode

            return mapperClassNode

        } catch (ex) {
            error getClass(), 'Problem building RowMapper ({}): {}', mapperName, ex.message
            throw ex
        }
    }
}