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
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithRelations
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithoutRelations
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static com.stehno.effigy.transform.util.AstUtils.methodN
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

import com.stehno.effigy.annotation.Repository
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier
/**
 * Created by cjstehno on 12/20/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class FindOperationsTransformer implements ASTTransformation {

    private static final String ENTITY_ID = 'entityId'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode repositoryNode = nodes[1] as ClassNode

        AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
        if (repositoryAnnot) {
            ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')
            info FindOperationsTransformer, 'Adding retrieve operations to repository ({})', repositoryNode.name

            injectCountMethod repositoryNode, entityNode
            injectExistsMethod repositoryNode, entityNode

            repositoryNode.allDeclaredMethods.findAll { m -> !m.static && m.abstract && m.name.startsWith('findBy') }.each { finder ->
                injectFinderMethod repositoryNode, entityNode, finder
            }

        } else {
            warn FindOperationsTransformer, 'FindOperations can only be applied to classes annotated with Effigy @Repository - ignored.'
        }
    }

    private static void injectFinderMethod(ClassNode repositoryNode, ClassNode entityNode, MethodNode methodNode) {
        info FindOperationsTransformer, 'Injecting finder ({}) into repository ({}).', methodNode.name, repositoryNode.name

        def criteriaParams = (methodNode.name - 'findBy').split('And').collect { "${it[0].toLowerCase()}${it[1..-1]}" }

        def entityTable = entityTable(entityNode)

        def wheres = criteriaParams.collect { propName ->
            def param = methodNode.parameters.find { it.name == propName as String }
            assert param, "Finder mismatch: No parameter exists for criteria ($propName)"

            def property = entityProperty(entityNode, propName as String)
            assert property, "Finder mismatch: No entity property exists for criteria ($propName)"

            "$entityTable.${property.columnName}=?"
        }

        def params = criteriaParams.collect { propName ->
            varX(propName)
        }

        methodNode.modifiers = Modifier.PUBLIC

        if (hasAssociatedEntities(entityNode)) {
            methodNode.code = block(
                query(
                    selectWithRelations(entityNode, wheres),
                    entityCollectionExtractor(entityNode),
                    params
                )
            )
        } else {
            methodNode.code = block(
                query(
                    selectWithoutRelations(entityNode, wheres),
                    entityRowMapper(entityNode),
                    params
                )
            )
        }

        if (!repositoryNode.hasMethod(methodNode.name, methodNode.parameters)) {
            repositoryNode.addMethod(methodNode)
        }
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
