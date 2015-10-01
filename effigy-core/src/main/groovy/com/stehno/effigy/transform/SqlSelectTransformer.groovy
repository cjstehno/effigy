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

import com.stehno.effigy.JdbcStrategy
import com.stehno.effigy.transform.model.SqlSelectModel
import com.stehno.effigy.transform.sql.SqlTemplate
import groovy.sql.Sql
import groovy.transform.TypeChecked
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.JdbcAccessor

import javax.sql.DataSource

import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION) @TypeChecked
class SqlSelectTransformer extends AbstractASTTransformation {

    // FIXME: add @ParamName annotation to use a replacement name different from the method parameter name

    // FIXME: row mapper support
    // FIXME: result set extractor support
    // FIXME: strategy support
    // FIXME: support for rename param or use as ordinal (via Param annot)

    // FIXME: sql should allow groovy templating
    // -- select a,b,c from stuff where d=:{foo.d} and e=:{foo.d * 2}


    private static final List<ClassNode> ALLOWED_SOURCE_TYPES = [
        make(DataSource), make(JdbcTemplate), make(JdbcOperations), make(JdbcAccessor), make(Sql)
    ].asImmutable()

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotationNode = nodes[0] as AnnotationNode
        MethodNode methodNode = nodes[1] as MethodNode

        SqlSelectModel model = SqlSelectModel.extract(annotationNode)

        VariableExpression sourceX = findSource(methodNode.declaringClass, model.source)
        SqlTemplate sqlTemplate = new SqlTemplate(model.sql)
        // TODO: should the model just provide this?

        methodNode.code = model.strategy == JdbcStrategy.SPRING ? springMethod() : groovyMethod(methodNode, sourceX, sqlTemplate)
    }

    static enum ReturnCategory {
        SINGLE, COLLECTION
    }

    private static ReturnCategory returnCategory(MethodNode methodNode) {
        /*
    void is error
    single object or primitive
    collection (Collection, Array)
    other is error
 */

    }

    private static Statement groovyMethod(MethodNode methodNode, VariableExpression sourceX, SqlTemplate sqlTemplate) {
        // FIXME: resolve the way the DataSource is provided

        Expression sqlX
        if (sourceX.type == ALLOWED_SOURCE_TYPES[0]) {
            // use DataSource
            sqlX = ctorX(make(Sql), args(sourceX))

        } else if (sourceX.type == ALLOWED_SOURCE_TYPES[1]) {
            // get .dataSource
            sqlX = ctorX(make(Sql), args(callX(sourceX, 'getDataSource')))

        } else if (sourceX.type == ALLOWED_SOURCE_TYPES[2]) {
            throw new IllegalArgumentException("JdbcOperations does not provide a DataSource for use in a Groovy strategy.")

        } else if (sourceX.type == ALLOWED_SOURCE_TYPES[3]) {
            // get .dataSource
            sqlX = ctorX(make(Sql), args(callX(sourceX, 'getDataSource')))

        } else if (sourceX.type == ALLOWED_SOURCE_TYPES[4]) {
            // use the Sql
            sqlX = sourceX
        }

        def code = block(
            declS(varX('_sql'), sqlX)
        )

        // FIXME: translate the sql and use the arguments
        // FIMXE: resolve the row mapper or extractor

        ReturnCategory returnCategory = returnCategory(methodNode)
        if (returnCategory == ReturnCategory.SINGLE) {
            // single return value
            // sql.firstRow(_sqlText, args...)

        } else {
            // collection return value
            // sql.eachRow(_sqlText, args){ row->
        }

        return code
    }

    private static Statement springMethod() {

    }

    // TODO: refactor this - it's ugly and not very efficient (probably extracted too)
    private static VariableExpression findSource(final ClassNode classNode, final String sourceName) {
        if (sourceName) {
            // search the types for a source with the given name

            PropertyNode sourceProperty = classNode.properties.find { pn ->
                pn.name == sourceName && pn.type in ALLOWED_SOURCE_TYPES
            }

            if (sourceProperty) {
                return varX(sourceName, newClass(sourceProperty.type))
            }

            FieldNode sourceField = classNode.fields.find { fn ->
                fn.name == sourceName && fn.type in ALLOWED_SOURCE_TYPES
            }

            if (sourceField) {
                return varX(sourceName, newClass(sourceField.type))
            }

            MethodNode methodNode = classNode.methods.find { mn ->
                mn.name == sourceName && mn.returnType in ALLOWED_SOURCE_TYPES
            }

            if (sourceField) {
                return varX(sourceName, newClass(methodNode.returnType))
            } else {
                throw new IllegalArgumentException("No source ($sourceName) found in class (${classNode.name}).")
            }

        } else {
            // search for types in order

            for (ClassNode sourceType in ALLOWED_SOURCE_TYPES) {
                String typeSourceName = "${sourceType.nameWithoutPackage[0].toLowerCase()}${sourceType.nameWithoutPackage[1..-1]}" as String

                PropertyNode sourceProperty = classNode.properties.find { pn -> pn.name == typeSourceName }

                if (sourceProperty) {
                    return varX(typeSourceName, newClass(sourceType))
                }

                FieldNode sourceField = classNode.fields.find { fn -> fn.name == typeSourceName }

                if (sourceField) {
                    return varX(typeSourceName, newClass(sourceType))
                }

                MethodNode methodNode = classNode.methods.find { mn -> mn.name == typeSourceName }

                if (sourceField) {
                    return varX(typeSourceName, newClass(sourceType))
                } else {
                    throw new IllegalArgumentException("No source (${sourceType.nameWithoutPackage} $typeSourceName) found in class (${classNode.name}).")
                }

            }
        }

    }
}


