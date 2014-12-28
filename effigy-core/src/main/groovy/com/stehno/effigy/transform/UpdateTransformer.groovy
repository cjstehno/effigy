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
import com.stehno.effigy.transform.model.VersionerPropertyModel
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.SqlBuilder.update
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static java.lang.reflect.Modifier.PROTECTED
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Transformer used to process the @Update annotations.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class UpdateTransformer extends MethodImplementingTransformation {

    private static final String ENTITY = 'entity'
    private static final String COMMA = ','
    private static final String ID = 'id'
    private static final String NEWLINE = '\n'

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType in [VOID_TYPE, int_TYPE, Integer_TYPE, boolean_TYPE, Boolean_TYPE]
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        ensureParameters methodNode

        String entityVar = ENTITY
        def entityCreator = null

        if (isEntityBased(methodNode.parameters, entityNode)) {
            entityVar = methodNode.parameters[0].name

        } else if (isMapBased(methodNode.parameters)) {
            entityCreator = declS(varX(entityVar), ctorX(entityNode, args(varX(methodNode.parameters[0].name))))

        } else {
            MapExpression argMap = new MapExpression()
            methodNode.parameters.each { mp ->
                argMap.addMapEntryExpression(new MapEntryExpression(constX(mp.name), varX(mp.name)))
            }

            entityCreator = declS(varX(entityVar), ctorX(entityNode, args(argMap)))
        }

        components(entityNode).each { ap ->
            injectComponentUpdateMethod repoNode, entityNode, ap
        }

        def vars = []

        entityProperties(entityNode, false).each { p ->
            if (p instanceof EmbeddedPropertyModel) {
                p.fieldNames.each { pf ->
                    vars << "${entityVar}.${p.propertyName}?.$pf"
                }
            } else {
                vars << "${entityVar}.${p.propertyName}"
            }
        }

        def code = block()

        if (entityCreator) {
            code.addStatement(entityCreator)
        }

        code.addStatement(codeS('''
                <% if(versioner){ %>
                def currentVersion = ${entity}.${versioner.propertyName} ?: 0
                ${entity}.${versioner.propertyName} = currentVersion + 1
                <% } %>

                int count = jdbcTemplate.update(
                    '$sql',
                    ${vars.join(',')},
                    ${entity}.${identifier.propertyName}
                    <% if(versioner){ %>
                        ,currentVersion
                    <% } %>
                )

                $o2m
                $o2o

                count
            ''',
            entity: entityVar,
            vars: vars,
            sql: sql(entityNode),
            table: entityTable(entityNode),
            versioner: versioner(entityNode),
            identifier: identifier(entityNode),
            o2m: associations(entityNode).collect { AssociationPropertyModel o2m ->
                "save${o2m.propertyName.capitalize()}(${entityVar})"
            }.join(NEWLINE),
            o2o: components(entityNode).collect { ComponentPropertyModel ap ->
                "update${ap.propertyName.capitalize()}(${entityVar}.${identifier(entityNode).propertyName}, ${entityVar}.${ap.propertyName})"
            }.join(NEWLINE)
        ))

        methodNode.modifiers = PUBLIC
        methodNode.code = code
    }

    private static void injectComponentUpdateMethod(ClassNode repositoryNode, ClassNode entityNode, ComponentPropertyModel o2op) {
        def methodName = "update${o2op.propertyName.capitalize()}"
        def methodParams = params(param(OBJECT_TYPE, ID), param(newClass(o2op.type), ENTITY))

        if (!repositoryNode.hasMethod(methodName, methodParams)) {
            def colUpdates = []
            def varUpdates = []

            components(entityNode).each { ap ->
                entityProperties(ap.type).collect { p ->
                    colUpdates << "${p.columnName} = ?"
                    varUpdates << "entity.${p.propertyName}"
                }.join(COMMA)
            }

            def statement = codeS(
                '''
                if( !id || !entity ){
                    jdbcTemplate.update('delete from $assocTable where $idCol = ?', id)
                } else {
                    int count = jdbcTemplate.update( 'update $assocTable set $updates where $idCol = ?', $vars,id )

                    if( count != 1 ){
                        throw new RuntimeException('Update count for $name (' + count + ') did not match expected count (1) - update failed.')
                    }
                }
            ''',
                name: o2op.propertyName,
                idCol: o2op.entityColumn,
                assocTable: o2op.lookupTable,
                updates: colUpdates.join(COMMA),
                vars: varUpdates.join(COMMA)
            )

            repositoryNode.addMethod(methodN(PROTECTED, methodName, VOID_TYPE, statement, methodParams))
        }
    }

    private static String sql(ClassNode entityNode) {
        def setters = []
        entityProperties(entityNode, false).each { p ->
            if (p instanceof EmbeddedPropertyModel) {
                p.columnNames.each { cn ->
                    setters << "$cn = ?"
                }
            } else {
                setters << "${p.columnName}=?"
            }
        }

        def wheres = ["${identifier(entityNode).columnName} = ?"]

        VersionerPropertyModel versionProperty = versioner(entityNode)
        if (versionProperty) {
            wheres << "${versionProperty.columnName} = ?"
        }

        update().table(entityTable(entityNode)).sets(setters).wheres(wheres).build()
    }

    private static void ensureParameters(MethodNode methodNode) {
        if (!methodNode.parameters) {
            throw new EffigyTransformationException('Update methods must accept at least an Entity or Map parameter.')
        }
    }

    private static boolean isEntityBased(Parameter[] parameters, ClassNode entityNode) {
        parameters[0].type == entityNode
    }

    private static boolean isMapBased(Parameter[] parameters) {
        parameters[0].type == MAP_TYPE
    }
}
/*
@Update()
  update TABLE set (col=val) where
  params should be entity or properties of entity (where non-entity are used in where clause)
  return type should be int for update count or boolean for updated/not-updated

@Update
boolean update(entity)

@Update - will use default where clause based on property name
int update(entity, lastName)

@Update('where @lastName like(:name)')
int update(entity, name)
 */