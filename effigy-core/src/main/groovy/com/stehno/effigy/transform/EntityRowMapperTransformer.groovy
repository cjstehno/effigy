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

import com.stehno.effigy.transform.jdbc.EntityRowMapper
import com.stehno.effigy.transform.model.ColumnPropertyModel
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.sql.ResultSet

import static com.stehno.effigy.transform.model.ColumnModelType.ID
import static com.stehno.effigy.transform.model.EntityModel.entityProperties
import static com.stehno.effigy.transform.util.AstUtils.*
import static com.stehno.effigy.transform.util.FieldTypeHandlerHelper.callReadFieldX
import static java.lang.reflect.Modifier.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass
import static org.codehaus.groovy.syntax.Token.newSymbol
import static org.codehaus.groovy.syntax.Types.EQUALS

/**
 * Transformer used for creating a <code>RowMapper</code> instance for the entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString') @Slf4j
class EntityRowMapperTransformer implements ASTTransformation {

    private static final String PREFIX = 'prefix'
    private static final String DATA = 'data'
    private static final String EMPTY_ENTITY = 'emptyEntity'
    private static final String ENTITY = 'entity'
    private static final String RS = 'rs'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode

        log.debug 'Creating RowMapper for: {}', entityClassNode.name

        ClassNode mapperClassNode = buildRowMapper(entityClassNode, source)
        injectRowMapperAccessor(entityClassNode, mapperClassNode)
    }

    /**
     * Creates a <code>RowMapper</code> implementation class for each annotated entity.
     *
     * @param model the entity model
     * @param source the source unit
     * @return the created RowMapper implementation class
     */
    @SuppressWarnings('GroovyAssignabilityCheck')
    private static ClassNode buildRowMapper(ClassNode entityNode, SourceUnit source) {
        String mapperName = "${entityNode.packageName}.${entityNode.nameWithoutPackage}RowMapper"
        try {
            ClassNode mapperClassNode = classN(mapperName, EntityRowMapper)

            mapperClassNode.addMethod(methodN(PROTECTED, 'newEntity', newClass(entityNode), returnS(ctorX(newClass(entityNode)))))

            def entityProps = entityProperties(entityNode)

            entityProps.each { ep ->
                if (ep instanceof EmbeddedPropertyModel) {
                    mapperClassNode.addMethod(methodN(
                        PRIVATE,
                        "new${ep.propertyName.capitalize()}",
                        newClass(ep.type),
                        returnS(ctorX(newClass(ep.type), args(varX(DATA)))),
                        params(param(makeClassSafe(Map), DATA))
                    ))

                    addEmbeddedMappingMethod(mapperClassNode, ep)

                } else {
                    if (ep.column.handler) {
                        mapperClassNode.addMethod(methodN(
                            PRIVATE,
                            "handle${ep.propertyName.capitalize()}",
                            newClass(ep.type),
                            returnS(callReadFieldX(ep, 'data')),
                            params(param(OBJECT_TYPE, 'data'))
                        ))
                    }

                    addColumnMappingMethod(mapperClassNode, ep)
                }
            }

            mapperClassNode.addMethod(methodN(PROTECTED, 'mapping', newClass(entityNode), block(
                declS(varX(EMPTY_ENTITY), constX(true)),
                *entityProps.collect { p ->
                    if (p instanceof ColumnPropertyModel && p.modelType == ID) {
                        ifElseS(
                            callThisX("map${p.propertyName.capitalize()}" as String, args(varX(RS), varX(ENTITY))),
                            returnS(constX(null)),
                            stmt(new BinaryExpression(varX(EMPTY_ENTITY), newSymbol(EQUALS, -1, -1), constX(false)))
                        )
                    } else {
                        ifS(
                            notX(
                                callThisX("map${p.propertyName.capitalize()}" as String, args(varX(RS), varX(ENTITY)))
                            ),
                            new BinaryExpression(varX(EMPTY_ENTITY), newSymbol(EQUALS, -1, -1), constX(false))
                        )
                    }
                },
                returnS(ternaryX(varX(EMPTY_ENTITY), constX(null), varX(ENTITY)))
            ), params(param(make(ResultSet), RS), param(OBJECT_TYPE, ENTITY))))

            source.AST.addClass(mapperClassNode)

            log.debug 'Injected row mapper ({}) for {}', mapperClassNode.name, entityNode

            return mapperClassNode

        } catch (ex) {
            log.error 'Problem building RowMapper ({}): {}', mapperName, ex.message
            throw ex
        }
    }

    private static void addColumnMappingMethod(ClassNode mapperNode, ColumnPropertyModel propertyModel) {
        mapperNode.addMethod(methodN(
            PRIVATE,
            "map${propertyModel.propertyName.capitalize()}",
            Boolean_TYPE,
            codeS(
                '''
                    def value = rs.getObject( prefix + '${p.column.name}' )
                    if( value != null ){
                        <% if( p.column.handler ){ %>
                            value = handle${p.propertyName.capitalize()}(value)
                        <% } %>

                        entity.${p.propertyName} = value
                        return false
                    }
                    return true
                ''',
                p: propertyModel
            ),
            params(param(make(ResultSet), RS), param(OBJECT_TYPE, ENTITY))
        ))
    }

    private static void addEmbeddedMappingMethod(ClassNode mapperNode, EmbeddedPropertyModel propertyModel) {
        mapperNode.addMethod(methodN(
            PRIVATE,
            "map${propertyModel.propertyName.capitalize()}",
            Boolean_TYPE,
            codeS(
                '''
                    def values = [
                        ${p.collectSubProperties { fld,col-> "$fld : rs.getObject(prefix + '$col')" }.join(',')}
                    ]
                    if( values.values().any { v-> v != null } ){
                        entity.${p.propertyName} = new${p.propertyName.capitalize()}( values )
                        return false
                    }
                    return true
                ''',
                p: propertyModel
            ),
            params(param(make(ResultSet), RS), param(OBJECT_TYPE, ENTITY))
        ))
    }

    /**
     * Injects a helper method into the entity. This helper method allows for simple retrieval of the generated row mapper with
     * optional prefix definition. The method signature is:
     *
     * <pre>public static rowMapper(String prefix='')</pre>
     *
     * @param entityClassNode
     * @param mapperClassNode
     * @param model
     */
    private static void injectRowMapperAccessor(ClassNode entityClassNode, ClassNode mapperClassNode) {
        entityClassNode.addMethod(methodN(
            PUBLIC | STATIC,
            'rowMapper',
            newClass(mapperClassNode),
            returnS(ctorX(newClass(mapperClassNode), args(new MapExpression([new MapEntryExpression(constX(PREFIX), varX(PREFIX))])))),
            [param(STRING_TYPE, PREFIX, constX(''))] as Parameter[]
        ))

        log.debug 'Injected row mapper helper method for {}', entityClassNode
    }
}
