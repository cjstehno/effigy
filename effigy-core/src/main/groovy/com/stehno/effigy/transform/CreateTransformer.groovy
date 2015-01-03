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

import com.stehno.effigy.transform.model.AssociationPropertyModel
import com.stehno.effigy.transform.model.ComponentPropertyModel
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.*
import static java.lang.reflect.Modifier.PROTECTED
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Transformer used to process the @Create annotation.
 */
@SuppressWarnings('GStringExpressionWithinString')
class CreateTransformer extends MethodImplementingTransformation {

    private static final String ID = 'id'
    private static final String ENTITY = 'entity'
    private static final String NEWLINE = '\n'
    private static final String COMMA = ','

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType == OBJECT_TYPE || identifier(entityNode).type == returnType
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        String entityVar = ENTITY
        def entityCreator = null

        if (isEntityParameter(methodNode.parameters, entityNode)) {
            entityVar = methodNode.parameters[0].name

        } else if (isMapParameter(methodNode.parameters)) {
            entityCreator = declS(varX(entityVar), ctorX(entityNode, args(varX(methodNode.parameters[0].name))))

        } else {
            MapExpression argMap = new MapExpression()
            methodNode.parameters.each { mp ->
                argMap.addMapEntryExpression(new MapEntryExpression(constX(mp.name), varX(mp.name)))
            }

            entityCreator = declS(varX(entityVar), ctorX(entityNode, args(argMap)))
        }

        components(entityNode).each { ap ->
            injectComponentSaveMethod repoNode, entityNode, ap
        }

        def versioner = versioner(entityNode)

        def values = []
        entityProperties(entityNode, false).each { pi ->
            if (pi instanceof EmbeddedPropertyModel) {
                pi.fieldNames.each { fn ->
                    values << "${entityVar}.${pi.propertyName}?.${fn}"
                }

            } else {
                if (pi.type.enum) {
                    values << "${entityVar}.${pi.propertyName}?.name()"
                } else {
                    values << "${entityVar}.${pi.propertyName}"
                }
            }
        }

        def statement = block(
            entityCreator ?: new EmptyStatement(),

            declS(varX('keys'), ctorX(make(GeneratedKeyHolder))),

            versioner ? codeS('${entity}.$name = 1', name: versioner.propertyName, entity: entityVar) : new EmptyStatement(),

            declS(varX('factory'), ctorX(make(PreparedStatementCreatorFactory), args(
                constX(
                    """insert into ${entityTable(entityNode)} (${columnNames(entityNode, false)}) values (${
                        columnPlaceholders(entityNode, false)
                    })""" as String
                ),
                arrayX(int_TYPE, columnTypes(entityNode, false).collect { typ ->
                    constX(typ)
                })
            ))),

            codeS(
                '''
                    def paramValues = [$values] as Object[]
                    jdbcTemplate.update(factory.newPreparedStatementCreator(paramValues), keys)
                    ${entity}.${idName} = keys.key

                    $o2m
                    $components

                    return keys.key
                    ''',
                entity: entityVar,
                values: values.join(COMMA),
                idName: identifier(entityNode).propertyName,
                o2m: associations(entityNode).collect { AssociationPropertyModel o2m ->
                    "save${o2m.propertyName.capitalize()}($entityVar)"
                }.join(NEWLINE),
                components: components(entityNode).collect { ComponentPropertyModel ap ->
                    "save${ap.propertyName.capitalize()}($entityVar.${identifier(entityNode).propertyName},$entityVar.${ap.propertyName})"
                }.join(NEWLINE)
            ),
        )

        updateMethod repoNode, methodNode, statement
    }

    private static void injectComponentSaveMethod(ClassNode repositoryNode, ClassNode entityNode, ComponentPropertyModel o2op) {
        def methodName = "save${o2op.propertyName.capitalize()}"
        def methodParams = [param(identifier(entityNode).type, ID), param(newClass(o2op.type), ENTITY)] as Parameter[]

        if (!repositoryNode.hasMethod(methodName, methodParams)) {
            try {
                def statement = codeS(
                    '''
                    if( !id || !entity ) return

                    int count = jdbcTemplate.update(
                        'insert into $assocTable ($assocColumns) values ($assocPlaceholders)',
                        $assocValues
                    )

                    if( count != 1 ){
                        throw new RuntimeException('Insert count for $name (' + count + ') did not match expected count (1) - save failed.')
                    }
                ''',
                    name: o2op.propertyName,
                    assocTable: o2op.lookupTable,
                    assocColumns: "${o2op.entityColumn},${columnNames(o2op.type)}",
                    assocPlaceholders: "?,${columnPlaceholders(o2op.type)}",
                    assocValues: ([ID] + entityProperties(o2op.type).collect {
                        "entity.${it.propertyName}"
                    }).join(COMMA)
                )

                repositoryNode.addMethod(methodN(PROTECTED, methodName, VOID_TYPE, statement, methodParams))

            } catch (ex) {
                error CreateTransformer, 'Unable to inject component save method for entity ({}): {}', entityNode.name, ex.message
                throw ex
            }
        }
    }

    private static boolean isEntityParameter(Parameter[] parameters, ClassNode entityNode) {
        parameters.size() == 1 && parameters[0].type == entityNode
    }

    private static boolean isMapParameter(Parameter[] parameters) {
        parameters.size() == 1 && parameters[0].type == MAP_TYPE
    }
}
