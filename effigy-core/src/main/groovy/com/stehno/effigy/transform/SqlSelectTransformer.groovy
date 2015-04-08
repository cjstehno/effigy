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

package com.stehno.effigy.transform

import com.stehno.effigy.annotation.PreparedStatementSetter
import com.stehno.effigy.annotation.ResultSetExtractor
import com.stehno.effigy.annotation.RowMapper
import com.stehno.effigy.transform.jdbc.RowMapperRegistry
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression

import static com.stehno.effigy.transform.ClassManipulationUtils.applyArguments
import static com.stehno.effigy.transform.SqlHelperAnnotation.resolveHelperX
import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.queryX
import static org.codehaus.groovy.ast.ClassHelper.VOID_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Transformer used to process <code>@SqlSelect</code> annotated methods.
 */
@Slf4j
class SqlSelectTransformer extends MethodImplementingTransformation {

    private static final RowMapperRegistry ROW_MAPPERS = new RowMapperRegistry()
    private static final String RESULTS = 'results'
    private static final String VALUE = 'value'

    SqlSelectTransformer() {
        entityRequired = false
    }

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType != VOID_TYPE
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        def sql = rawSql(extractString(annotationNode, VALUE)).parameters(methodNode.parameters)
        def setter = resolveHelperX(repoNode, methodNode, PreparedStatementSetter, org.springframework.jdbc.core.PreparedStatementSetter)
        def extractor = resolveHelperX(repoNode, methodNode, ResultSetExtractor, org.springframework.jdbc.core.ResultSetExtractor)

        //noinspection GroovyAssignabilityCheck
        Expression qx = queryX(
            sql.build(),
            applyArguments(
                methodNode,
                ResultSetExtractor,
                code,
                extractor
            ) ?: applyArguments(methodNode, RowMapper, code, resolveRowMapper(repoNode, methodNode)),
            setter ? applyArguments(methodNode, PreparedStatementSetter, code, setter) : sql.params
        )

        if (extractor) {
            // assume extractors do their own return type conversion
            code.addStatement(returnS(qx))

        } else {
            if (methodNode.returnType.isArray() || methodNode.returnType.interfaces.contains(make(Collection))) {
                // just return the results and let groovy handle casting
                code.addStatement(returnS(qx))

            } else {
                // return the first element in the results
                code.addStatement(declS(varX(RESULTS), qx))
                code.addStatement(returnS(callX(varX(RESULTS), 'get', args(constX(0)))))
            }
        }

        updateMethod repoNode, methodNode, code
    }

    private static Expression resolveRowMapper(final ClassNode repoNode, final MethodNode methodNode) {
        resolveHelperX(
            repoNode,
            methodNode,
            RowMapper,
            org.springframework.jdbc.core.RowMapper
        ) ?: ROW_MAPPERS.findRowMapper(resolveReturnType(methodNode))
    }

    private static ClassNode resolveReturnType(final MethodNode methodNode) {
        def returnType = methodNode.returnType

        if (methodNode.returnType.isUsingGenerics()) {
            returnType = methodNode.returnType.genericsTypes[0].type
        }

        log.debug 'Resolved return type ({}) for method ({}).', returnType.name, methodNode.name

        returnType
    }
}

