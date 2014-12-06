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
import static com.stehno.effigy.logging.Logger.warn
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.annotation.EffigyRepository
import com.stehno.effigy.transform.model.IdentifierPropertyModel
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 12/6/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class RetrieveOperationsTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(EffigyRepository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')
            info RetrieveOperationsTransformer, 'Adding retrieve operations to repository ({})', repositoryNode.name

            injectRetrieveMethod repositoryNode, entityNode
            injectRetrieveAllMethod repositoryNode, entityNode

        } else {
            warn CreateOperationsTransformer, 'RetrieveOperations can only be applied to classes annotated with @EffigyRepository - ignored.'
        }
    }

    /**
     * Injects the retrieve method into an entity repository class. This method is an implementation of the
     * CrudOperations.retrieve(E entityId) method.
     *
     * @param repositoryClassNode
     * @param model
     */
    static void injectRetrieveMethod(final ClassNode repositoryClassNode, final ClassNode entityNode) {
        info RetrieveOperationsTransformer, 'Injecting retrieve method into repository for {}', entityNode.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'retrieve',
                Modifier.PUBLIC,
                newClass(entityNode),
                [new Parameter(identifier(entityNode).type, 'entityId')] as Parameter[],
                null,
                hasAssociatedEntities(entityNode) ? retrieveSingleWitRelations(entityNode) : retrieveSingleWithoutRelations(entityNode)
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }

    private static Statement retrieveSingleWithoutRelations(ClassNode entityNode) {
        block(
            declS(varX('mapper'), callX(classX(newClass(entityNode)), 'rowMapper')),

            codeS(
                '''
                    jdbcTemplate.queryForObject(
                        'select ${columnNames} from ${table} where ${identifier.columnName}=?',
                        mapper,
                        entityId
                    )
                    ''',
                table: entityTable(entityNode),
                identifier: identifier(entityNode),
                columnNames: columnNames(entityNode)
            )
        )
    }

    private static Statement retrieveSingleWitRelations(ClassNode entityNode) {
        block(
            declS(varX('extractor'), callX(classX(newClass(entityNode)), 'associationExtractor')),

            codeS(
                '''
                    jdbcTemplate.query(
                        '$sql',
                        extractor,
                        entityId
                    )
                    ''',
                sql: sqlWithRelations(entityNode, true)
            )
        )
    }

    static void injectRetrieveAllMethod(final ClassNode repositoryClassNode, ClassNode entityNode) {
        info RetrieveOperationsTransformer, 'Injecting retrieve All method into repository for {}', entityNode.name

        try {
            repositoryClassNode.addMethod(new MethodNode(
                'retrieveAll',
                Modifier.PUBLIC,
                makeClassSafe(List),
                [] as Parameter[],
                null,
                hasAssociatedEntities(entityNode) ? retrieveAllWithRelations(entityNode) : retrieveAllWithoutRelations(entityNode)
            ))

        } catch (ex) {
            ex.printStackTrace()
        }
    }

    private static Statement retrieveAllWithRelations(ClassNode entityNode) {
        block(
            declS(varX('extractor'), callX(classX(newClass(entityNode)), 'collectionAssociationExtractor')),

            codeS(
                '''
                    jdbcTemplate.query(
                        '$sql',
                        extractor
                    )
                    ''',
                sql: sqlWithRelations(entityNode)
            )
        )
    }

    private static Statement retrieveAllWithoutRelations(ClassNode entityNode) {
        block(
            declS(varX('mapper'), callX(classX(newClass(entityNode)), 'rowMapper')),

            codeS(
                '''
                    jdbcTemplate.query(
                        'select ${columnNames} from ${table}',
                        mapper
                    )
                ''',
                table: entityTable(entityNode),
                columnNames: columnNames(entityNode)
            )
        )
    }

    private static String sqlWithRelations(ClassNode entityNode, final boolean single = false) {
        String sql = 'select '

        String entityTableName = entityTable(entityNode)
        def entityIdentifier = identifier(entityNode)

        entityProperties(entityNode).each { p ->
            sql += "${entityTableName}.${p.columnName} as ${entityTableName}_${p.columnName},"
        }

        sql += oneToManyAssociations(entityNode).collect { ap ->
            String associatedTable = entityTable(ap.associatedType)

            entityProperties(ap.associatedType).collect { p ->
                "${associatedTable}.${p.columnName} as ${ap.propertyName}_${p.columnName}"
            }.join(',')

        }.join(',')

        sql += " from ${entityTableName}"

        oneToManyAssociations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)
            IdentifierPropertyModel associatedIdentifier = identifier(ap.associatedType)

            sql += " LEFT OUTER JOIN ${ap.table} on ${ap.table}.${ap.entityId}=${entityTableName}.${entityIdentifier.columnName}"
            sql += " LEFT OUTER JOIN ${associatedTable} on ${ap.table}.${ap.associationId}=${associatedTable}.${associatedIdentifier.columnName}"
        }

        if (single) {
            sql += " where ${entityTableName}.${entityIdentifier.columnName}=?"
        }

        sql
    }
}
