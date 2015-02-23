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

import com.stehno.effigy.annotation.RowMapper
import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.sql.RawSqlBuilder
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.SingleColumnRowMapper

import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.query
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics

/**
 * Transformer used to process @SqlSelect annotated methods.
 */
class SqlSelectTransformer extends MethodImplementingTransformation {

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType != VOID_TYPE
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        def sql = resolveSql(extractString(annotationNode, 'value'), methodNode.parameters)

        code.addStatement(query(
            sql.build(),
            resolveResultSetExtractor() ?: resolveRowMapper(repoNode, methodNode),
            sql.params
        ))

        updateMethod repoNode, methodNode, code
    }

    private static RawSqlBuilder resolveSql(final String sql, final Parameter[] parameters) {
        def builder = rawSql(sql)

        parameters.each { param ->
            builder.param(param.name, varX(param.name, param.type))
        }

        builder
    }

    private static Expression resolveRowMapper(final ClassNode repoNode, final MethodNode methodNode) {
        def mapper = null

        def mapperAnnot = methodNode.getAnnotations(make(RowMapper))[0]
        if (mapperAnnot) {
            String beanName = extractString(mapperAnnot, 'bean', '')
            String mapperType = extractString(mapperAnnot, 'type', '')
            String mapperFactory = extractString(mapperAnnot, 'factory', '')

            if (beanName) {
                // FIXME: type - inject shared instance method

            } else if (mapperType && mapperFactory) {
                // TODO: type+factory - inject shared instance method (this is just duplicated each use)
                mapper = callX(make(mapperType), mapperFactory)

            } else if (mapperType) {
                // TODO: bean - inject shared bean instance method (this is just duplicated each use)
                mapper = ctorX(make(mapperType))
            }
        }

        mapper ?: findRegisteredRowMapper(resolveReturnType(methodNode))
    }

    private static Expression resolveResultSetExtractor() {
        // TODO: add in support for extractor annotation
        null
    }

    private static ClassNode resolveReturnType(final MethodNode methodNode) {
        def returnType = methodNode.returnType

        if (methodNode.returnType.isUsingGenerics()) {
            returnType = methodNode.returnType.genericsTypes[0].type
        }

        Logger.info SqlSelectTransformer, 'Resolved return type ({}) for method ({}).', returnType.name, methodNode.name

        returnType
    }

    // TODO: this should be refactored out someplace else
    private static final MAPPER_REGISTRY = prepareMapperRegistry()

    private static Map<ClassNode, Expression> prepareMapperRegistry() {
        def mapperRegistry = [:]

        singleColumnRowMapperX(Integer_TYPE).with { m ->
            mapperRegistry.put int_TYPE, m
            mapperRegistry.put Integer_TYPE, m
        }

        singleColumnRowMapperX(Long_TYPE).with { m ->
            mapperRegistry.put long_TYPE, m
            mapperRegistry.put Long_TYPE, m
        }

        singleColumnRowMapperX(Boolean_TYPE).with { m ->
            mapperRegistry.put boolean_TYPE, m
            mapperRegistry.put Boolean_TYPE, m
        }

        mapperRegistry.put STRING_TYPE, singleColumnRowMapperX(STRING_TYPE)

        // ... date, byte, short, float, double

        // TODO: other types to be added... (maybe allow custom registration?)
        // TODO: also document the types supported and what they map to in user guide

        mapperRegistry
    }

    private static Expression findRegisteredRowMapper(final ClassNode returnType) {
        // TODO: document that the fallback is bean property mapper
        MAPPER_REGISTRY.get(returnType) ?: ctorX(makeClassSafeWithGenerics(BeanPropertyRowMapper, returnType), args(constX(returnType.typeClass)))
    }

    private static Expression singleColumnRowMapperX(ClassNode targetType) {
        // originally had an args block to specify the return type to the constructor but that has compilation issues
        ctorX(makeClassSafeWithGenerics(SingleColumnRowMapper, targetType))
    }
}
