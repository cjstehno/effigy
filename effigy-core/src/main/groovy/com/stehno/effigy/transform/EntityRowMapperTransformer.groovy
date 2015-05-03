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
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.sql.ResultSet

import static com.stehno.effigy.transform.model.EntityModel.embeddedEntityProperties
import static com.stehno.effigy.transform.model.EntityModel.entityProperties
import static com.stehno.effigy.transform.util.AstUtils.*
import static com.stehno.effigy.transform.util.FieldTypeHandlerHelper.callReadFieldX
import static java.lang.reflect.Modifier.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Transformer used for creating a <code>RowMapper</code> instance for the entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString') @Slf4j
class EntityRowMapperTransformer implements ASTTransformation {

    private static final String PREFIX = 'prefix'
    private static final String DATA = 'data'

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
    private static ClassNode buildRowMapper(ClassNode entityNode, SourceUnit source) {
        String mapperName = "${entityNode.packageName}.${entityNode.nameWithoutPackage}RowMapper"
        try {
            ClassNode mapperClassNode = classN(mapperName, EntityRowMapper)

            mapperClassNode.addMethod(methodN(PROTECTED, 'newEntity', newClass(entityNode), returnS(ctorX(newClass(entityNode)))))

            embeddedEntityProperties(entityNode).each { emb ->
                mapperClassNode.addMethod(methodN(
                    PROTECTED,
                    "new${emb.propertyName.capitalize()}",
                    newClass(emb.type),
                    returnS(ctorX(newClass(emb.type), args(varX(DATA)))),
                    [param(makeClassSafe(Map), DATA)] as Parameter[]
                ))

                addEmbeddedMappingMethod(mapperClassNode, emb)
            }

            def entityProps = entityProperties(entityNode)

            // add the custom field mappers (if any)
            entityProps.findAll { ep -> !(ep instanceof EmbeddedPropertyModel) }.each { ColumnPropertyModel ep ->
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

            mapperClassNode.addMethod(methodN(PROTECTED, 'mapping', newClass(entityNode), block(
                codeS(
                    '''
                        def emptyEntity = true
                        <%  props.each { p-> %>
                                if( !map${p.propertyName.capitalize()}(rs,entity) ) emptyEntity = false
                        <%  } %>
                            return emptyEntity ? null : entity
                        ''',
                    props: entityProps
                )
            ), [param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[]))

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
            params(param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity'))
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
            params(param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity'))
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
