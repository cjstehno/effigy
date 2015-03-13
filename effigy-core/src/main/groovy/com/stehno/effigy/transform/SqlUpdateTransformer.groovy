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
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression

import static com.stehno.effigy.transform.ClassManipulationUtils.applyArguments
import static com.stehno.effigy.transform.SqlHelperAnnotation.resolveHelperX
import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.updateX
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS

/**
 * Transformer used to process @SqlUpdate annotated methods.
 */
class SqlUpdateTransformer extends MethodImplementingTransformation {

    private static final String VALUE = 'value'

    SqlUpdateTransformer() {
        entityRequired = false
    }

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType in [VOID_TYPE, int_TYPE, long_TYPE, boolean_TYPE]
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code = block()

        def sql = rawSql(extractString(annotationNode, VALUE)).parameters(methodNode.parameters)
        def setter = resolveHelperX(repoNode, methodNode, PreparedStatementSetter, org.springframework.jdbc.core.PreparedStatementSetter)

        //noinspection GroovyAssignabilityCheck
        Expression qx = updateX(
            sql.build(),
            setter ? applyArguments(methodNode, PreparedStatementSetter, code, setter) : sql.params
        )

        if (methodNode.returnType != VOID_TYPE) {
            code.addStatement(returnS(qx))
        }

        updateMethod repoNode, methodNode, code
    }
}

