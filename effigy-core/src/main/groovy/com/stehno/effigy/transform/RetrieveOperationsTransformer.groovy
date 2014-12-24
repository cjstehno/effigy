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
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithAssociations
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithoutAssociations
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.annotation.Repository
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
@SuppressWarnings('GStringExpressionWithinString')
class RetrieveOperationsTransformer implements ASTTransformation {

    private static final String ENTITY_ID = 'entityId'

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
                [param(identifier(entityNode).type, ENTITY_ID)] as Parameter[],
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
            queryForObject(
                selectWithoutAssociations(entityNode, ["${identifier(entityNode).columnName}=?"]),
                entityRowMapper(entityNode),
                [varX(ENTITY_ID)]
            )
        )
    }

    private static Statement retrieveSingleWitRelations(ClassNode entityNode) {
        block(
            query(
                selectWithAssociations(entityNode, ["${entityTable(entityNode)}.${identifier(entityNode).columnName}=?"]),
                entityExtractor(entityNode),
                [varX(ENTITY_ID)]
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
            query(
                selectWithAssociations(entityNode),
                entityCollectionExtractor(entityNode)
            )
        )
    }

    private static Statement retrieveAllWithoutRelations(ClassNode entityNode) {
        block(
            query(
                selectWithoutAssociations(entityNode),
                entityRowMapper(entityNode)
            )
        )
    }
}
