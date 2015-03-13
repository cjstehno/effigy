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
import groovy.transform.Immutable
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext

import static com.stehno.effigy.transform.SelectHelperAnnotation.helperFrom
import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static com.stehno.effigy.transform.util.AnnotationUtils.*
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

        Expression qx
        if (setter) {
            qx = queryX(
                sql.build(),
                applyArguments(
                    methodNode,
                    ResultSetExtractor,
                    code,
                    extractor
                ) ?: applyArguments(methodNode, RowMapper, code, resolveRowMapper(repoNode, methodNode)),
                applyArguments(methodNode, PreparedStatementSetter, code, setter)
            )

        } else {
            qx = queryX(
                sql.build(),
                applyArguments(
                    methodNode,
                    ResultSetExtractor,
                    code,
                    extractor
                ) ?: applyArguments(methodNode, RowMapper, code, resolveRowMapper(repoNode, methodNode)),
                sql.params
            )
        }

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
            String varName = "_${helperType.simpleName}"

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

    // FIXME: there is still a lot of code overlap with the helpers - refactor away
    // - maybe pull the resolve methods into a shared one in the immutable stub class?
    private static Expression resolveSetter(final ClassNode repoNode, final MethodNode methodNode) {
        def setter = null

        def annot = helperFrom(methodNode, PreparedStatementSetter)
        if (annot) {
            if (annot.bean) {
                setter = applyAutowiredBean(repoNode, org.springframework.jdbc.core.PreparedStatementSetter, annot.bean, annot.singleton)

            } else if (annot.type != VOID_TYPE && annot.factory) {
                setter = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.PreparedStatementSetter,
                    "setter${annot.type.nameWithoutPackage}From${annot.factory.capitalize()}",
                    callX(annot.type, annot.factory),
                    annot.singleton
                )

            } else if (annot.type != VOID_TYPE) {
                setter = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.PreparedStatementSetter,
                    "setter${annot.type.nameWithoutPackage}",
                    ctorX(annot.type),
                    annot.singleton
                )
            }
        }

        setter
    }

    private static Expression resolveRowMapper(final ClassNode repoNode, final MethodNode methodNode) {
        def mapper = null

        def annot = helperFrom(methodNode, RowMapper)
        if (annot) {
            if (annot.bean) {
                mapper = applyAutowiredBean(repoNode, org.springframework.jdbc.core.RowMapper, annot.bean, annot.singleton)

            } else if (annot.type != VOID_TYPE && annot.factory) {
                mapper = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.RowMapper,
                    "mapper${annot.type.nameWithoutPackage}From${annot.factory.capitalize()}",
                    callX(annot.type, annot.factory),
                    annot.singleton
                )

            } else if (annot.type != VOID_TYPE) {
                mapper = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.RowMapper,
                    "mapper${annot.type.nameWithoutPackage}",
                    ctorX(annot.type),
                    annot.singleton
                )
            }
        }

        mapper ?: ROW_MAPPERS.findRowMapper(resolveReturnType(methodNode))
    }

    private static Expression resolveResultSetExtractor(ClassNode repoNode, MethodNode methodNode) {
        def extractor = null

        def annot = helperFrom(methodNode, ResultSetExtractor)
        if (annot) {
            if (annot.bean) {
                extractor = applyAutowiredBean(repoNode, org.springframework.jdbc.core.ResultSetExtractor, annot.bean, annot.singleton)

            } else if (annot.type != VOID_TYPE && annot.factory) {
                extractor = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.ResultSetExtractor,
                    "extractor${annot.type.nameWithoutPackage}From${annot.factory.capitalize()}",
                    callX(annot.type, annot.factory),
                    annot.singleton
                )

            } else if (annot.type != VOID_TYPE) {
                extractor = applySharedField(
                    repoNode,
                    org.springframework.jdbc.core.ResultSetExtractor,
                    "extractor${annot.type.nameWithoutPackage}",
                    ctorX(annot.type),
                    annot.singleton
                )
            }
        }

        extractor
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

@Immutable(knownImmutableClasses = [ClassNode])
class SelectHelperAnnotation {

    private static final String BEAN = 'bean'
    private static final String TYPE = 'type'
    private static final String FACTORY = 'factory'
    private static final String DEFAULT_EMPTY = ''

    String bean
    ClassNode type
    String factory
    boolean singleton
    boolean arguments

    static SelectHelperAnnotation helperFrom(MethodNode methodNode, Class annotType) {
        def annot = methodNode.getAnnotations(make(annotType))[0]

        if (annot) {
            boolean args = extractBoolean(annot, 'arguments', false)

            return new SelectHelperAnnotation(
                extractString(annot, BEAN, DEFAULT_EMPTY),
                extractClass(annot, TYPE),
                extractString(annot, FACTORY, DEFAULT_EMPTY),
                args ? false : extractBoolean(annot, 'singleton', true),
                args
            )
        }

        return null
    }
}