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
import com.stehno.effigy.annotation.PreparedStatementSetter
import com.stehno.effigy.annotation.ResultSetExtractor
import com.stehno.effigy.annotation.RowMapper
import com.stehno.effigy.jdbc.RowMapperRegistry
import com.stehno.effigy.logging.Logger
import com.stehno.effigy.transform.sql.RawSqlBuilder
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext

import static SqlHelperAnnotation.helperFrom
import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
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
    private static final String VALUE = 'value'
    private static final String APPLICATION_CONTEXT = 'applicationContext'
    private static final String SET_METHOD_ARGUMENTS = 'setMethodArguments'

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

        def sql = resolveSql(extractString(annotationNode, VALUE), methodNode.parameters)
        def setter = resolveSetter(repoNode, methodNode)
        def extractor = resolveResultSetExtractor(repoNode, methodNode)

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

    private static Expression applyArguments(MethodNode methodNode, Class helperType, BlockStatement code, Expression generator) {
        if (helperFrom(methodNode, helperType)?.arguments) {
            String varName = "__${helperType.simpleName}"

            // need to replace the generator expression with a variable
            code.addStatement(declS(varX(varName), generator))
            generator = varX(varName)

            code.addStatement(stmt(callX(varX(varName), SET_METHOD_ARGUMENTS, new MapExpression(methodNode.parameters.collect { p ->
                new MapEntryExpression(constX(p.name), varX(p.name))
            }))))
        }
        generator
    }

    private static RawSqlBuilder resolveSql(final String sql, final Parameter[] parameters) {
        def builder = rawSql(sql)

        parameters.each { param ->
            builder.param(param.name, varX(param.name, param.type))
        }

        builder
    }

    private static FieldNode addRepoField(ClassNode repoNode, String name, Class helperType, Expression initializer) {
        def node = new FieldNode(name, PRIVATE, makeClassSafe(helperType), repoNode, initializer)
        repoNode.addField(node)
        node
    }

    private static void autowireField(FieldNode fieldNode, String beanName = null) {
        fieldNode.addAnnotation(new AnnotationNode(make(Autowired)))

        if (beanName) {
            def annotNode = new AnnotationNode(make(Qualifier))
            annotNode.setMember(VALUE, constX(beanName))
            fieldNode.addAnnotation(annotNode)
        }
    }

    private static Expression applyAutowiredBean(ClassNode repoNode, Class helperType, String name, boolean singleton) {
        if (singleton) {
            if (!repoHasField(repoNode, name)) {
                FieldNode fieldNode = addRepoField repoNode, name, helperType, new EmptyExpression()
                autowireField fieldNode, name

                repoNode.addProperty(new PropertyNode(fieldNode, PUBLIC, null, null))
            }

            return varX(name)

        } else {
            if (!repoHasField(repoNode, APPLICATION_CONTEXT)) {
                FieldNode contextNode = addRepoField(repoNode, APPLICATION_CONTEXT, ApplicationContext, new EmptyExpression())
                autowireField(contextNode)
            }

            return callX(varX(APPLICATION_CONTEXT), 'getBean', args(constX(name), classX(org.springframework.jdbc.core.PreparedStatementSetter)))
        }
    }

    private static Expression applySharedField(ClassNode repoNode, Class helperType, String name, Expression generator, boolean singleton) {
        if (singleton) {
            if (!repoHasField(repoNode, name)) {
                addRepoField repoNode, name, helperType, generator
            }

            return varX(name)

        } else {
            return generator
        }
    }

    private static Expression resolveSetter(final ClassNode repoNode, final MethodNode methodNode) {
        resolveHelper(repoNode, methodNode, PreparedStatementSetter, org.springframework.jdbc.core.PreparedStatementSetter)
    }

    private static Expression resolveRowMapper(final ClassNode repoNode, final MethodNode methodNode) {
        resolveHelper(
            repoNode,
            methodNode,
            RowMapper,
            org.springframework.jdbc.core.RowMapper
        ) ?: ROW_MAPPERS.findRowMapper(resolveReturnType(methodNode))
    }

    private static Expression resolveResultSetExtractor(ClassNode repoNode, MethodNode methodNode) {
        resolveHelper(repoNode, methodNode, ResultSetExtractor, org.springframework.jdbc.core.ResultSetExtractor)
    }

    private static Expression resolveHelper(ClassNode repoNode, MethodNode methodNode, Class helperType, Class helperInterface) {
        def mapper = null

        def annot = helperFrom(methodNode, helperType)
        if (annot) {
            if (annot.bean) {
                mapper = applyAutowiredBean(repoNode, helperInterface, annot.bean, annot.singleton)

            } else if (annot.type && annot.factory) {
                mapper = applySharedField(
                    repoNode,
                    helperInterface,
                    "_${annot.type.nameWithoutPackage}_${annot.factory.capitalize()}",
                    callX(annot.type, annot.factory),
                    annot.singleton
                )

            } else if (annot.type) {
                mapper = applySharedField(repoNode, helperInterface, "_${annot.type.nameWithoutPackage}", ctorX(annot.type), annot.singleton)
            }
        }

        mapper
    }

    private static FieldNode repoHasField(ClassNode repoNode, String fieldName) {
        repoNode.fields.find { f -> f.name == fieldName }
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

