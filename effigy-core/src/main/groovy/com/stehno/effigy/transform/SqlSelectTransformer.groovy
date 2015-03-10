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
import com.stehno.effigy.jdbc.RowMapperRegistry
import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.sql.RawSqlBuilder
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.queryX
import static java.lang.reflect.Modifier.PRIVATE
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.ClassHelper.VOID_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
/**
 * Transformer used to process @SqlSelect annotated methods.
 */
class SqlSelectTransformer extends MethodImplementingTransformation {

    private static final RowMapperRegistry ROW_MAPPERS = new RowMapperRegistry()
    private static final String RESULTS = 'results'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType != VOID_TYPE
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        def sql = resolveSql(extractString(annotationNode, 'value'), methodNode.parameters)

        Expression qx = queryX(
            sql.build(),
            resolveResultSetExtractor() ?: resolveRowMapper(repoNode, methodNode),
            sql.params
        )

        if (methodNode.returnType.isArray() || methodNode.returnType.interfaces.contains(make(Collection))) {
            // just return the results and let groovy handle casting
            code.addStatement(returnS(qx))

        } else {
            // return the first element in the results
            code.addStatement(declS(varX(RESULTS), qx))
            code.addStatement(returnS(callX(varX(RESULTS), 'get', args(constX(0)))))
        }

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
            ClassNode mapperType = extractClass(mapperAnnot, 'type')
            String mapperFactory = extractString(mapperAnnot, 'factory', '')

            if (beanName) {
                FieldNode fieldNode = new FieldNode(
                    beanName,
                    PRIVATE,
                    makeClassSafe(org.springframework.jdbc.core.RowMapper),
                    repoNode,
                    new EmptyExpression()
                )

                fieldNode.addAnnotation(new AnnotationNode(make(Autowired)))

                def annotNode = new AnnotationNode(make(Qualifier))
                annotNode.setMember('value', constX(beanName))
                fieldNode.addAnnotation(annotNode)

                repoNode.addProperty(new PropertyNode(fieldNode, PUBLIC, null, null))

                mapper = varX(beanName)

            } else if (mapperType != VOID_TYPE && mapperFactory) {
                String fieldName = "mapper${mapperType.nameWithoutPackage}From${mapperFactory.capitalize()}"

                if (!repoNode.fields.find { f -> f.name == fieldName }) {
                    repoNode.addField(new FieldNode(
                        fieldName,
                        PRIVATE,
                        makeClassSafe(org.springframework.jdbc.core.RowMapper),
                        repoNode,
                        callX(mapperType, mapperFactory)
                    ))
                }

                mapper = varX(fieldName)

            } else if (mapperType != VOID_TYPE) {
                String fieldName = "mapper${mapperType.nameWithoutPackage}"

                if (!repoNode.fields.find { f -> f.name == fieldName }) {
                    repoNode.addField(new FieldNode(
                        fieldName,
                        PRIVATE,
                        makeClassSafe(org.springframework.jdbc.core.RowMapper),
                        repoNode,
                        ctorX(mapperType)
                    ))
                }

                mapper = varX(fieldName)
            }
        }

        mapper ?: ROW_MAPPERS.findRowMapper(resolveReturnType(methodNode))
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
}
