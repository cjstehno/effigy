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

package com.stehno.effigy.transform.model

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.transform.EffigyTransformationException
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression

import java.sql.Types

import static com.stehno.effigy.transform.util.AnnotationUtils.*
import static com.stehno.effigy.transform.util.StringUtils.camelCaseToUnderscore
import static java.sql.Types.*
import static org.codehaus.groovy.ast.ClassHelper.VOID_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make

/**
 * Model for the database column information of an entity.
 */
@Immutable(knownImmutableClasses = [ClassNode]) @Slf4j
class ColumnModel {

    /**
     * The column name.
     */
    String name

    /**
     * The expression representing the SQL type (int) value to be used.
     */
    int type

    /**
     * The column conversion handler class.
     */
    ClassNode handler

    @SuppressWarnings('GroovyAssignabilityCheck')
    static ColumnModel extract(final FieldNode fieldNode) {
        AnnotationNode columnAnnot = fieldNode.getAnnotations(make(Column))[0]

        if (columnAnnot) {
            ClassNode handlerNode = extractClass(columnAnnot, 'handler')

            return new ColumnModel(
                extractString(columnAnnot, 'value') ?: camelCaseToUnderscore(fieldNode.name),
                resolveSqlType(extractTypeValue(columnAnnot), fieldNode),
                handlerNode == VOID_TYPE ? null : handlerNode
            )
        }

        return new ColumnModel(
            camelCaseToUnderscore(fieldNode.name),
            resolveDefaultSqlType(fieldNode),
            null
        )
    }

    private static Integer extractTypeValue(final AnnotationNode columnAnnot) {
        Expression typeExpression = extractExpression(columnAnnot, 'type')
        if (!typeExpression) {
            return Integer.MIN_VALUE

        } else if (typeExpression instanceof PropertyExpression) {
            // Extracting expressions from annotations is a bit of a pain. Here we can assume/force the use of a java.sql.Types constant value.
            return Types."${(typeExpression.property as ConstantExpression).value}" as int

        } else if (typeExpression instanceof ConstantExpression) {
            return typeExpression.value

        } else {
            throw new EffigyTransformationException("Column annotation 'type' field does not support: $typeExpression")
        }
    }

    private static int resolveSqlType(final Integer annotType, final FieldNode fieldNode) {
        annotType != Integer.MIN_VALUE ? annotType : resolveDefaultSqlType(fieldNode)
    }

    private static int resolveDefaultSqlType(final FieldNode fieldNode) {
        if (fieldNode.type.enum) {
            return VARCHAR
        }

        switch (fieldNode.type.name) {
            case 'java.lang.String': return VARCHAR
            case 'java.sql.Date':
            case 'java.util.Date':
                return TIMESTAMP
            case 'java.lang.Boolean':
            case 'boolean':
                return BOOLEAN
            case 'java.lang.Integer':
            case 'int':
                return INTEGER
            case 'java.lang.Long':
            case 'long':
                return BIGINT
            case 'java.lang.Double':
            case 'double':
                return DOUBLE
            case 'java.lang.Float':
            case 'float':
                return FLOAT
            case 'java.lang.Short':
            case 'short':
                return TINYINT
            default: return JAVA_OBJECT
        }
    }
}
