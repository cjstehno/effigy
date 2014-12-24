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

import com.stehno.effigy.annotation.Limit
import com.stehno.effigy.annotation.Limited
import com.stehno.effigy.annotation.Repository
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier

import static com.stehno.effigy.logging.Logger.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithAssociations
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithoutAssociations
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AnnotationUtils.extractInteger
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Injects the finder operations into an Effigy Repository.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class FindOperationsTransformer implements ASTTransformation {

    private static final String ENTITY_ID = 'entityId'
    private static final String FIND_BY = 'findBy'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')
            info FindOperationsTransformer, 'Adding retrieve operations to repository ({})', repositoryNode.name

            injectCountMethod repositoryNode, entityNode
            injectExistsMethod repositoryNode, entityNode

            repositoryNode.allDeclaredMethods.findAll { m -> !m.static && m.abstract && m.name.startsWith(FIND_BY) }.each { finder ->
                injectFinderMethod repositoryNode, entityNode, finder
            }

        } else {
            warn FindOperationsTransformer, 'FindOperations can only be applied to classes annotated with Effigy @Repository - ignored.'
        }
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    private static void injectFinderMethod(ClassNode repositoryNode, ClassNode entityNode, MethodNode methodNode) {
        info FindOperationsTransformer, 'Injecting finder ({}) into repository ({}).', methodNode.name, repositoryNode.name
        try {
            def (whereCriteria, whereParams) = resolveCriteria(methodNode, entityNode)

            def (limitSql, limitParam) = resolveLimitation(methodNode)

            methodNode.modifiers = Modifier.PUBLIC

            if (hasAssociatedEntities(entityNode)) {
                methodNode.code = block(
                    query(
                        selectWithAssociations(entityNode, whereCriteria),
                        entityCollectionExtractor(entityNode, limitParam),
                        whereParams
                    )
                )
            } else {
                if (limitParam) {
                    whereParams << limitParam
                }

                methodNode.code = block(
                    query(
                        selectWithoutAssociations(entityNode, whereCriteria, limitSql),
                        entityRowMapper(entityNode),
                        whereParams
                    )
                )
            }

            if (!repositoryNode.hasMethod(methodNode.name, methodNode.parameters)) {
                repositoryNode.addMethod(methodNode)
            }

        } catch (ex) {
            error FindOperationsTransformer, 'Problem injecting finder ({}): {}', methodNode.name, ex.message
            throw ex
        }
    }

    private static List resolveLimitation(MethodNode methodNode) {
        def limitSql = null
        def limitParam = null

        def annotation = methodNode.getAnnotations(make(Limited))[0]
        if (annotation) {
            Integer limitValue = extractInteger(annotation, 'value')
            limitSql = '?'
            limitParam = constX(limitValue)

        } else {
            def limitInput = methodNode.parameters.find { p -> p.getAnnotations(make(Limit)) }
            if (limitInput) {
                limitSql = '?'
                limitParam = varX(limitInput.name)
            }
        }

        [limitSql, limitParam]
    }

    private static List resolveCriteria(MethodNode methodNode, ClassNode entityNode) {
        def entityTable = entityTable(entityNode)

        def whereCriteria = []
        def whereParams = []

        (methodNode.name - FIND_BY).split('And').collect { "${it[0].toLowerCase()}${it[1..-1]}" }.each { propName ->
            def param = methodNode.parameters.find { it.name == propName as String }
            assert param, "Finder mismatch: No parameter exists for criteria ($propName)"

            def property = entityProperty(entityNode, propName as String)
            assert property, "Finder mismatch: No entity property exists for criteria ($propName)"

            whereCriteria << "$entityTable.${property.columnName}=?"

            if (property.type.enum) {
                whereParams << callX(varX(propName), 'name')
            } else {
                whereParams << varX(propName)
            }
        }

        [whereCriteria, whereParams]
    }

    /**
     */
    private static void injectCountMethod(final ClassNode repositoryNode, final ClassNode entityNode) {
        info FindOperationsTransformer, 'Injecting count method into repository for {}', entityNode.name

        try {
            repositoryNode.addMethod(methodN(
                Modifier.PUBLIC,
                'count',
                ClassHelper.int_TYPE,
                block(codeS(
                    '''
                        if( entityId ){
                            jdbcTemplate.queryForObject('select count(*) from $table where $idCol=?', Integer, entityId)
                        } else {
                            jdbcTemplate.queryForObject('select count(*) from $table', Integer)
                        }
                    ''',
                    table: entityTable(entityNode),
                    idCol: identifier(entityNode).columnName
                )),
                [param(identifier(entityNode).type, ENTITY_ID, constX(null))] as Parameter[]
            ))
        } catch (ex) {
            error FindOperationsTransformer, 'Unable to inject count method into repository ({}): {}', repositoryNode.name, ex.message
            throw ex
        }
    }

    /**
     */
    private static void injectExistsMethod(final ClassNode repositoryNode, final ClassNode entityNode) {
        info FindOperationsTransformer, 'Injecting exists method into repository for {}', entityNode.name

        try {
            repositoryNode.addMethod(methodN(
                Modifier.PUBLIC,
                'exists',
                ClassHelper.boolean_TYPE,
                block(codeS(
                    'jdbcTemplate.queryForObject(\'select $idCol from $table where $idCol=?\', Object, entityId) != null',
                    table: entityTable(entityNode),
                    idCol: identifier(entityNode).columnName
                )),
                [param(identifier(entityNode).type, ENTITY_ID)] as Parameter[]
            ))
        } catch (ex) {
            error FindOperationsTransformer, 'Unable to inject exists method into repository ({}): {}', repositoryNode.name, ex.message
            throw ex
        }
    }
}
