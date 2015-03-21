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
import com.stehno.effigy.jdbc.EffigyEntityRowMapper
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

import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.*
import static java.lang.reflect.Modifier.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass
/**
 * Transformer used for creating a RowMapper instance for the Component associations of an Entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString') @Slf4j
class ComponentRowMapperTransformer implements ASTTransformation {

    private static final String PREFIX = 'prefix'
    private static final String MAP = 'map'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityNode = nodes[1] as ClassNode

        log.debug 'Visiting {} for association row mapper creation.', entityNode.name

        components(entityNode).each { ap ->
            log.debug 'Visiting one-to-one association ({}).', ap.type.name

            if (!isEntity(ap.type) && !rowMapperExists(ap.type, source)) {
                log.info 'Creating one-to-one Association RowMapper for: {}', ap.type.name

                injectRowMapperAccessor(ap.type, buildRowMapper(ap.type, source))
            }
        }
    }

    private static boolean rowMapperExists(ClassNode assocNode, SourceUnit source) {
        String mapperName = rowMapperName(assocNode)
        source.AST.classes.find { cn -> cn.name == mapperName }
    }

    private static GString rowMapperName(ClassNode assocNode) {
        "${assocNode.packageName}.${assocNode.nameWithoutPackage}RowMapper"
    }

    /**
     * Creates a <code>RowMapper</code> implementation class for each annotated entity.
     *
     * @param model the entity model
     * @param source the source unit
     * @return the created RowMapper implementation class
     */
    private static ClassNode buildRowMapper(ClassNode assocNode, SourceUnit source) {
        String mapperName = rowMapperName(assocNode)

        try {
            ClassNode mapperClassNode = classN(mapperName, EffigyEntityRowMapper)

            // TODO: need to refactor this away - since its not used here
            mapperClassNode.addMethod(methodN(PROTECTED, 'newEntity', newClass(assocNode), returnS(ctorX(newClass(assocNode)))))

            mapperClassNode.addMethod(methodN(
                PROTECTED,
                'createEntity',
                newClass(assocNode),
                returnS(ctorX(newClass(assocNode), args(varX(MAP)))),
                [param(makeClassSafe(Map), MAP)] as Parameter[]
            ))

            mapperClassNode.addMethod(methodN(PROTECTED, 'mapping', newClass(assocNode), block(
                codeS(
                    '''
                        def map = [:]
                        <%  props.each { p-> %>
                                map.${p.propertyName} = rs.getObject( prefix + '${p.columnName}' )
                        <%  } %>
                        return map.find { k,v-> v != null } ? createEntity(map) : null
                    ''',
                    props: entityProperties(assocNode)
                )
            ), [param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[]))

            source.AST.addClass(mapperClassNode)

            log.info 'Injected row mapper ({}) for {}', mapperClassNode.name, assocNode

            return mapperClassNode

        } catch (ex) {
            log.error 'Problem building RowMapper ({}): {}', mapperName, ex.message
            throw ex
        }
    }

    /**
     * Injects a helper method into the entity. This helper method allows for simple retrieval of the generated row mapper with
     * optional prefix definition. The method signature is:
     *
     * <pre>public static rowMapper(String prefix='')</pre>
     *
     * @param entityNode
     * @param mapperClassNode
     * @param model
     */
    private static void injectRowMapperAccessor(ClassNode assocNode, ClassNode mapperClassNode) {
        assocNode.addMethod(methodN(
            PUBLIC | STATIC,
            'rowMapper',
            newClass(mapperClassNode),
            returnS(ctorX(newClass(mapperClassNode), args(new MapExpression([new MapEntryExpression(constX(PREFIX), varX(PREFIX))])))),
            [param(STRING_TYPE, PREFIX, constX(''))] as Parameter[]
        ))

        log.info 'Injected row mapper helper method for {}', assocNode.name
    }
}
