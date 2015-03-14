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

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ClassExpression

import static com.stehno.effigy.transform.model.EntityModel.entityTable
import static com.stehno.effigy.transform.sql.SelectSqlBuilder.select
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.queryForObject
import static org.codehaus.groovy.ast.ClassHelper.Integer_TYPE
import static org.codehaus.groovy.ast.ClassHelper.int_TYPE

/**
 * Transformer used to process the @Count annotations.
 */
class CountTransformer extends MethodImplementingTransformation {

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType in [int_TYPE, Integer_TYPE]
    }

    @Override
    @SuppressWarnings('GroovyAssignabilityCheck')
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def sql = select().column('count(*)').from(entityTable(entityNode))

        applyParameters(sql, new AnnotatedMethod(annotationNode, entityNode, methodNode))

        updateMethod repoNode, methodNode, queryForObject(
            sql.build(),
            new ClassExpression(Integer_TYPE),
            sql.params
        )
    }
}

