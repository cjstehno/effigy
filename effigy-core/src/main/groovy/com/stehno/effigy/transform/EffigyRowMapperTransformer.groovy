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

import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.model.EntityModelUtils.entityProperties
import static com.stehno.effigy.transform.model.EntityModelUtils.identifier
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.jdbc.EffigyEntityRowMapper
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier
import java.sql.ResultSet

/**
 * Transformer used for processing the EffigyRepository annotation - creates a RowMapper instance for the entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyRowMapperTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = extractClass(nodes[0] as AnnotationNode, 'forEntity')

        info EffigyRowMapperTransformer, 'Creating RowMapper for: {}', entityClassNode.name

        ClassNode mapperClassNode = buildRowMapper(entityClassNode, source)
        injectRowMapperAccessor(entityClassNode, mapperClassNode)
    }

    /**
     * Creates a RowMapper implementation class for each annotated entity.
     *
     * @param model the entity model
     * @param source the source unit
     * @return the created RowMapper implementation class
     */
    private static ClassNode buildRowMapper(ClassNode entityNode, SourceUnit source) {
        ClassNode mapperClassNode = null
        try {
            mapperClassNode = new ClassNode(
                "${entityNode.packageName}.${entityNode.nameWithoutPackage}RowMapper",
                Modifier.PUBLIC,
                makeClassSafe(EffigyEntityRowMapper),
                [] as ClassNode[],
                [] as MixinNode[]
            )

            mapperClassNode.addMethod(new MethodNode(
                'newEntity',
                Modifier.PROTECTED,
                newClass(entityNode),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(ctorX(newClass(entityNode)))
            ))

            mapperClassNode.addMethod(new MethodNode(
                'mapping',
                Modifier.PROTECTED,
                newClass(entityNode),
                [param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[],
                [] as ClassNode[],
                block(
                    codeS(
                        '''
                        <% props.each { p-> %>
                            entity.${p.propertyName} = rs.getObject( prefix + '${p.columnName}' )
                        <% } %>
                        return ( entity.${identifier.propertyName} ? entity : null )
                        ''',
                        props: entityProperties(entityNode),
                        identifier: identifier(entityNode)
                    )
                )
            ))

            source.AST.addClass(mapperClassNode)

            info EffigyRowMapperTransformer, 'Injected row mapper ({}) for {}', mapperClassNode.name, entityNode

        } catch (ex) {
            ex.printStackTrace()
        }
        mapperClassNode
    }

    /**
     * Injects a helper method into the entity. This helper method allows for simple retrieval of the generated row mapper with
     * optional prefix definition. The method signature is:
     *
     * public static rowMapper(String prefix='')
     *
     * @param entityClassNode
     * @param mapperClassNode
     * @param model
     */
    private static void injectRowMapperAccessor(ClassNode entityClassNode, ClassNode mapperClassNode) {
        entityClassNode.addMethod(new MethodNode(
            'rowMapper',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(mapperClassNode),
            [new Parameter(STRING_TYPE, 'prefix', constX(''))] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(mapperClassNode), args(new MapExpression([new MapEntryExpression(constX('prefix'), varX('prefix'))]))))
        ))

        info EffigyRowMapperTransformer, 'Injected row mapper helper method for {}', entityClassNode
    }
}
