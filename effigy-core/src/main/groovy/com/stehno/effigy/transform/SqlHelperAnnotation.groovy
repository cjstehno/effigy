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

import com.stehno.effigy.transform.util.AnnotationUtils
import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.PreparedStatementSetter

import static com.stehno.effigy.transform.ClassManipulationUtils.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Immutable object stub used to represent the Sql helper annotation values during the transformation.
 */
@Immutable(knownImmutableClasses = [ClassNode])
class SqlHelperAnnotation {

    private static final String BEAN = 'bean'
    private static final String TYPE = 'type'
    private static final String FACTORY = 'factory'
    private static final String DEFAULT_EMPTY = ''
    private static final String APPLICATION_CONTEXT = 'applicationContext'

    String bean
    ClassNode type
    String factory
    boolean singleton
    boolean arguments

    static SqlHelperAnnotation helperFrom(MethodNode methodNode, Class annotType) {
        def annot = methodNode.getAnnotations(ClassHelper.make(annotType))[0]

        if (annot) {
            boolean args = AnnotationUtils.extractBoolean(annot, 'arguments', false)
            ClassNode helperNode = AnnotationUtils.extractClass(annot, TYPE)

            return new SqlHelperAnnotation(
                AnnotationUtils.extractString(annot, BEAN, DEFAULT_EMPTY),
                helperNode == ClassHelper.VOID_TYPE ? null : helperNode,
                AnnotationUtils.extractString(annot, FACTORY, DEFAULT_EMPTY),
                args ? false : AnnotationUtils.extractBoolean(annot, 'singleton', true),
                args
            )
        }

        return null
    }

    static Expression resolveHelperX(ClassNode repoNode, MethodNode methodNode, Class helperType, Class helperInterface) {
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

    private static Expression applyAutowiredBean(ClassNode repoNode, Class helperType, String name, boolean singleton) {
        if (singleton) {
            if (!hasField(repoNode, name)) {
                FieldNode fieldNode = addSafeField repoNode, helperType, name, new EmptyExpression()
                autowireField fieldNode, name
                addFieldProperty repoNode, fieldNode
            }

            return varX(name)
        }

        if (!hasField(repoNode, APPLICATION_CONTEXT)) {
            FieldNode contextNode = addSafeField(repoNode, ApplicationContext, APPLICATION_CONTEXT, new EmptyExpression())
            autowireField contextNode
            addFieldProperty repoNode, contextNode
        }

        return callX(varX(APPLICATION_CONTEXT), 'getBean', args(constX(name), classX(PreparedStatementSetter)))
    }

    private static Expression applySharedField(ClassNode repoNode, Class helperType, String name, Expression generator, boolean singleton) {
        if (singleton) {
            if (!hasField(repoNode, name)) {
                addSafeField repoNode, helperType, name, generator
            }

            return varX(name)
        }

        return generator
    }
}
