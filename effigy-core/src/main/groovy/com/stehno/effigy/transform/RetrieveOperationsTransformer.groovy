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
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.transform.model.EmbeddedPropertyModel
import com.stehno.effigy.transform.model.IdentifierPropertyModel
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

/**
 * Transform used to inject the Retrieve CRUD operations.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class RetrieveOperationsTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
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
            error RetrieveOperationsTransformer, 'Unable to inject retrieve methods into repository ({}): {}', repositoryClassNode.name, ex.message
            throw ex
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
            error RetrieveOperationsTransformer, 'Unable to inject retrieveAll method into repository ({}): {}', repositoryClassNode.name, ex.message
            throw ex
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
            if (p instanceof EmbeddedPropertyModel) {
                p.columnNames.each { cn ->
                    sql += "${entityTableName}.${cn} as ${entityTableName}_${cn},"
                }
            } else {
                sql += "${entityTableName}.${p.columnName} as ${entityTableName}_${p.columnName},"
            }
        }

        def assocFields = []

        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)

            entityProperties(ap.associatedType).each { p ->
                if (p instanceof EmbeddedPropertyModel) {
                    p.columnNames.each { cn ->
                        assocFields << "${associatedTable}.${cn} as ${ap.propertyName}_${cn}"
                    }
                } else {
                    assocFields << "${associatedTable}.${p.columnName} as ${ap.propertyName}_${p.columnName}"
                }
            }
        }

        sql += assocFields.join(',')

        String componentFields = components(entityNode).collect { ap ->
            entityProperties(ap.type).collect { p ->
                "${ap.lookupTable}.${p.columnName} as ${ap.propertyName}_${p.columnName}"
            }.join(',')
        }.join(',')

        if (componentFields) {
            sql += ",$componentFields"
        }

        sql += " from ${entityTableName}"

        associations(entityNode).each { ap ->
            String associatedTable = entityTable(ap.associatedType)
            IdentifierPropertyModel associatedIdentifier = identifier(ap.associatedType)

            sql += " LEFT OUTER JOIN ${ap.joinTable} on ${ap.joinTable}.${ap.entityColumn}=${entityTableName}.${entityIdentifier.columnName}"
            sql += " LEFT OUTER JOIN ${associatedTable} on ${ap.joinTable}.${ap.assocColumn}=${associatedTable}.${associatedIdentifier.columnName}"
        }

        components(entityNode).each { ap ->
            sql += " LEFT OUTER JOIN ${ap.lookupTable} on ${ap.lookupTable}.${ap.entityColumn}=${entityTableName}.${entityIdentifier.columnName}"
        }

        if (single) {
            sql += " where ${entityTableName}.${entityIdentifier.columnName}=?"
        }


        trace RetrieveOperationsTransformer, '------------------------------'
        trace RetrieveOperationsTransformer, 'Sql for entity ({}): {}', entityNode.name, sql
        trace RetrieveOperationsTransformer, '------------------------------'

        sql
    }
}
