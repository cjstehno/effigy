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
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.transform.model.EntityModel
import com.stehno.effigy.transform.model.EntityModelRegistry
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement

import java.lang.reflect.Modifier

/**
 * Code generators for the entity repository retrieval methods provided by the CrudOperations interface.
 */
class RetrieveMethodInjector {

    /**
     * Injects the retrieve method into an entity repository class. This method is an implementation of the
     * CrudOperations.retrieve(E entityId) method.
     *
     * @param repositoryClassNode
     * @param model
     */
    static void injectRetrieveMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        info RetrieveMethodInjector, 'Injecting retrieve method into repository for {}', model.type.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'retrieve',
                Modifier.PUBLIC,
                newClass(model.type),
                [new Parameter(model.identifier.type, 'entityId')] as Parameter[],
                null,
                model.hasAssociations() ? retrieveSingleWitRelations(model) : retrieveSingleWithoutRelations(model)
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }

    private static Statement retrieveSingleWithoutRelations(final EntityModel model) {
        block(
            declS(varX('mapper'), callX(classX(newClass(model.type)), 'rowMapper')),

            codeS(
                '''
                    jdbcTemplate.queryForObject(
                        'select ${model.columnNames().join(',')} from ${model.table} where ${model.identifier.columnName}=?',
                        mapper,
                        entityId
                    )
                    ''',
                model: model
            )
        )
    }

    private static Statement retrieveSingleWitRelations(final EntityModel model) {
        block(
            declS(varX('extractor'), callX(classX(newClass(model.type)), 'associationExtractor')),

            codeS(
                '''
                    jdbcTemplate.query(
                        '$sql',
                        extractor,
                        entityId
                    )
                    ''',
                model: model,
                sql: sqlWithRelations(model, true)
            )
        )
    }

    static void injectRetrieveAllMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        info RetrieveMethodInjector, 'Injecting retrieve All method into repository for {}', model.type.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'retrieveAll',
                Modifier.PUBLIC,
                makeClassSafe(List),
                [] as Parameter[],
                null,
                model.hasAssociations() ? retrieveAllWithRelations(model) : retrieveAllWithoutRelations(model)
            ))

        } catch (ex) {
            ex.printStackTrace()
        }
    }

    private static Statement retrieveAllWithRelations(final EntityModel model) {
        block(
            declS(varX('extractor'), callX(classX(newClass(model.type)), 'collectionAssociationExtractor')),

            codeS(
                '''
                    jdbcTemplate.query(
                        '$sql',
                        extractor
                    )
                    ''',
                model: model,
                sql: sqlWithRelations(model)
            )
        )
    }

    private static Statement retrieveAllWithoutRelations(final EntityModel model) {
        block(
            declS(varX('mapper'), callX(classX(newClass(model.type)), 'rowMapper')),

            codeS(
                '''
                    jdbcTemplate.query(
                        'select ${model.columnNames().join(',')} from ${model.table}',
                        mapper
                    )
                ''',
                model: model
            )
        )
    }

    private static String sqlWithRelations(final EntityModel model, final boolean single = false) {
        String sql = 'select '

        model.findProperties().each { p ->
            sql += "${model.table}.${p.columnName} as ${model.table}_${p.columnName},"
        }

        sql += model.findAssociationProperties().collect { ap ->
            def associatedModel = EntityModelRegistry.instance.lookup(ap.associatedType)

            associatedModel.findProperties().collect { p ->
                "${associatedModel.table}.${p.columnName} as ${ap.propertyName}_${p.columnName}"
            }.join(',')

        }.join(',')

        sql += " from ${model.table}"

        model.findAssociationProperties().each { ap ->
            def associatedModel = EntityModelRegistry.instance.lookup(ap.associatedType)

            sql += " LEFT OUTER JOIN ${ap.table} on ${ap.table}.${ap.entityId}=${model.table}.${model.identifier.columnName}"
            sql += " LEFT OUTER JOIN ${associatedModel.table} on ${ap.table}.${ap.associationId}=${associatedModel.table}.${associatedModel.identifier.columnName}"
        }

        if (single) {
            sql += " where ${model.table}.${model.identifier.columnName}=?"
        }

        sql
    }
}
