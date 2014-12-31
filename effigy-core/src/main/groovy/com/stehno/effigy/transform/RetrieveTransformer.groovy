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

import com.stehno.effigy.annotation.Limit
import com.stehno.effigy.annotation.Offset
import com.stehno.effigy.transform.sql.SqlTemplate
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import static com.stehno.effigy.transform.model.EntityModel.hasAssociatedEntities
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithAssociations
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithoutAssociations
import static com.stehno.effigy.transform.util.AnnotationUtils.extractInteger
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.ClassHelper.int_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

/**
 * Transformer used to process the @Retrieve annotations.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class RetrieveTransformer extends MethodImplementingTransformation {

    private static final String RESULTS = 'results'
    private static final String PLACEHOLDER = '?'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType == entityNode || returnType.implementsInterface(makeClassSafe(Collection))
    }

    @Override
    @SuppressWarnings('GroovyAssignabilityCheck')
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def (wheres, params) = extractParameters(annotationNode, entityNode, methodNode)
        def orders = extractOrders(annotationNode, entityNode)
        def (offset, offsetParam) = extractOffset(annotationNode, methodNode)
        def (limit, limitParam) = extractLimit(annotationNode, methodNode)

        def code = block()

        def sqlParams = params
        if (limitParam) {
            sqlParams << limitParam
        }
        if (offsetParam) {
            sqlParams << offsetParam
        }

        if (hasAssociatedEntities(entityNode)) {
            code.addStatement declS(varX(RESULTS), queryX(
                // FIXME: needs limit support
                // FIXME: needs offset support
                selectWithAssociations(entityNode, wheres, orders),
                entityCollectionExtractor(entityNode),
                params
            ))
        } else {
            code.addStatement declS(varX(RESULTS), queryX(
                selectWithoutAssociations(entityNode, wheres, limit, offset, orders),
                entityRowMapper(entityNode),
                params
            ))
        }

        if (methodNode.returnType == entityNode) {
            code.addStatement(returnS(callX(varX(RESULTS), 'getAt', constX(0))))
        } else {
            code.addStatement(returnS(varX(RESULTS)))
        }

        methodNode.modifiers = PUBLIC
        methodNode.code = code
    }

    // TODO: refactor this and the limit into a shared codebase
    private static List extractOffset(AnnotationNode annotationNode, MethodNode methodNode) {
        def offset = null
        def param = null

        def offsetParam = findOffsetParameter(methodNode)

        Integer offsetValue = extractInteger(annotationNode, 'offset')
        if (offsetValue > -1) {
            offset = PLACEHOLDER
            param = constX(offsetValue)

        } else if (offsetParam) {
            offset = PLACEHOLDER
            param = varX(offsetParam.name)
        }

        [offset, param]
    }

    private static List extractLimit(AnnotationNode annotationNode, MethodNode methodNode) {
        def limit = null
        def param = null

        def limitParam = findLimitParameter(methodNode)

        Integer limitValue = extractInteger(annotationNode, 'limit')
        if (limitValue > -1) {
            limit = PLACEHOLDER
            param = constX(limitValue)

        } else if (limitParam) {
            limit = PLACEHOLDER
            param = varX(limitParam.name)
        }

        [limit, param]
    }

    private static Parameter findLimitParameter(MethodNode methodNode) {
        methodNode.parameters.find { p -> p.getAnnotations(make(Limit)) && p.type == int_TYPE }
    }

    private static Parameter findOffsetParameter(MethodNode methodNode) {
        methodNode.parameters.find { p -> p.getAnnotations(make(Offset)) && p.type == int_TYPE }
    }

    private static String extractOrders(AnnotationNode annotationNode, ClassNode entityNode) {
        String orders = null

        String orderTemplate = extractString(annotationNode, 'order')
        if (orderTemplate) {
            orders = new SqlTemplate(orderTemplate).sql(entityNode)
        } else {
            // FIXME: support for runtime order param
        }

        orders
    }
}
