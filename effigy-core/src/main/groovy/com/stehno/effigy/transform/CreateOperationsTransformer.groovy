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

import static com.stehno.effigy.logging.Logger.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AnnotationUtils.hasAnnotation
import static com.stehno.effigy.transform.util.AstUtils.arrayX
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.EffigyRepository
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.OneToManyPropertyModel
import com.stehno.effigy.transform.model.OneToOnePropertyModel
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import java.lang.reflect.Modifier

/**
 * Transformer used to process the @CreateOperations annotation - injects CRUD create methods.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class CreateOperationsTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(EffigyRepository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')

            info CreateOperationsTransformer, 'Adding create operations to repository ({})', repositoryNode.name

            injectCreateMethod repositoryNode, entityNode

        } else {
            warn CreateOperationsTransformer, 'CreateOperations can only be applied to classes annotated with @EffigyRepository - ignored.'
        }
    }

    private static void injectCreateMethod(final ClassNode repositoryClassNode, final ClassNode entityNode) {
        info CreateOperationsTransformer, 'Injecting create method into repository for {}', entityNode.name
        try {
            oneToManyAssociations(entityNode).each { OneToManyPropertyModel o2m ->
                injectO2MSaveMethod(repositoryClassNode, entityNode, o2m)
            }

            oneToOneAssociations(entityNode).each { ap ->
                injectO2OSaveMethod repositoryClassNode, entityNode, ap
            }

            def versioner = versioner(entityNode)

            def values = []
            entityProperties(entityNode, false).each { pi ->
                if (pi instanceof EmbeddedPropertyModel) {
                    pi.fieldNames.each { fn ->
                        values << "entity.${pi.propertyName}?.${fn}"
                    }

                } else {
                    if (pi.type.enum) {
                        values << "entity.${pi.propertyName}?.name()"
                    } else {
                        values << "entity.${pi.propertyName}"
                    }
                }
            }

            def statement = block(
                declS(varX('keys'), ctorX(make(GeneratedKeyHolder))),

                versioner ? codeS('entity.$name = 0', name: versioner.propertyName) : new EmptyStatement(),

                declS(varX('factory'), ctorX(make(PreparedStatementCreatorFactory), args(
                    constX("insert into ${entityTable(entityNode)} (${columnNames(entityNode, false)}) values (${columnPlaceholders(entityNode, false)})" as String),
                    arrayX(int_TYPE, columnTypes(entityNode, false).collect { typ ->
                        constX(typ)
                    })
                ))),

                codeS(
                    '''
                        def paramValues = [$values] as Object[]
                        jdbcTemplate.update(factory.newPreparedStatementCreator(paramValues), keys)
                        entity.${idName} = keys.key

                        $o2m
                        $o2o

                        return keys.key
                    ''',
                    values: values.join(','),
                    idName: identifier(entityNode).propertyName,
                    o2m: oneToManyAssociations(entityNode).collect { OneToManyPropertyModel o2m ->
                        "save${o2m.propertyName.capitalize()}(entity)"
                    }.join('\n'),
                    o2o: oneToOneAssociations(entityNode).collect { OneToOnePropertyModel ap ->
                        "save${ap.propertyName.capitalize()}(entity.${identifier(entityNode).propertyName},entity.${ap.propertyName})"
                    }.join('\n')
                ),
            )

            repositoryClassNode.addMethod(new MethodNode(
                'create',
                Modifier.PUBLIC,
                identifier(entityNode).type,
                [new Parameter(newClass(entityNode), 'entity')] as Parameter[],
                null,
                statement
            ))

        } catch (ex) {
            error CreateOperationsTransformer, 'Unable to inject create operations for entity ({}): ', entityNode.name, ex.message
            throw ex
        }
    }

    private static void injectO2OSaveMethod(ClassNode repositoryNode, ClassNode entityNode, OneToOnePropertyModel o2op) {
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
            assocTable: o2op.table,
            assocColumns: "${o2op.identifierColumn},${columnNames(o2op.type)}",
            assocPlaceholders: "?,${columnPlaceholders(o2op.type)}",
            assocValues: (["id"] + entityProperties(o2op.type).collect { "entity.${it.propertyName}" }).join(',')
        )

        repositoryNode.addMethod(new MethodNode(
            "save${o2op.propertyName.capitalize()}",
            Modifier.PROTECTED,
            ClassHelper.VOID_TYPE,
            [param(OBJECT_TYPE, 'id'), param(newClass(entityNode), 'entity')] as Parameter[],
            null,
            statement
        ))
    }

    // TODO: this should probably be pulled into a common area since update uses the same method
    private static void injectO2MSaveMethod(ClassNode repositoryClassNode, ClassNode entityNode, OneToManyPropertyModel o2m) {
        def statement = codeS(
            '''
                int expects = entity.${name}.size()
                int count = 0
                def ent = entity

                jdbcTemplate.update('delete from $assocTable where $tableEntIdName=?', ent.${entityIdName})

                entity.${name}.each { itm->
                    count += jdbcTemplate.update(
                        'insert into $assocTable ($tableEntIdName,$tableAssocIdName) values (?,?)',
                        ent.${entityIdName},
                        itm.${assocIdName}
                    )
                }

                if( count != expects ){
                    entity.${entityIdName} = 0
                    throw new RuntimeException('Insert count for $name (' + count + ') did not match expected count (' + expects + ') - save failed.')
                }
            ''',

            name: o2m.propertyName,
            assocTable: o2m.table,
            tableEntIdName: o2m.entityId,
            tableAssocIdName: o2m.associationId,
            entityIdName: identifier(entityNode).propertyName,
            assocIdName: findIdName(o2m.type)
        )

        repositoryClassNode.addMethod(new MethodNode(
            "save${o2m.propertyName.capitalize()}",
            Modifier.PROTECTED,
            ClassHelper.VOID_TYPE,
            [new Parameter(newClass(entityNode), 'entity')] as Parameter[],
            null,
            statement
        ))
    }

    // FIXME: this should be part of the model (?)
    private static findIdName(ClassNode classNode) {
        GenericsType mappedType = classNode.genericsTypes.find { hasAnnotation(it.type, EffigyEntity) }
        mappedType.type.fields.find { hasAnnotation(it, Id) }.name
    }
}
