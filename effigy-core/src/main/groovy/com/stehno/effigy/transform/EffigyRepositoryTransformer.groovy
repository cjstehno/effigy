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
import static com.stehno.effigy.logging.Logger.trace
import static com.stehno.effigy.transform.CreateMethodInjector.injectCreateMethod
import static com.stehno.effigy.transform.DeleteMethodInjector.injectDeleteAllMethod
import static com.stehno.effigy.transform.DeleteMethodInjector.injectDeleteMethod
import static com.stehno.effigy.transform.RetrieveMethodInjector.injectRetrieveAllMethod
import static com.stehno.effigy.transform.RetrieveMethodInjector.injectRetrieveMethod
import static com.stehno.effigy.transform.UpdateMethodInjector.injectUpdateMethod
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass

import com.stehno.effigy.repository.CrudOperations
import com.stehno.effigy.transform.model.EntityModel
import com.stehno.effigy.transform.model.EntityModelRegistry
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

import java.lang.reflect.Modifier

/**
 * Transformer used for processing the EffigyRepository annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyRepositoryTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode effigyAnnotNode = nodes[0] as AnnotationNode
        ClassNode repositoryClassNode = nodes[1] as ClassNode

        boolean implementsCrud = repositoryClassNode.implementsInterface(new ClassNode(CrudOperations))
        info EffigyRepositoryTransformer, 'Implements CRUD: {}', implementsCrud

        injectJdbcTemplate repositoryClassNode
        removeAbstract repositoryClassNode

        ClassNode entityClassNode = extractClass(effigyAnnotNode, 'forEntity')
        info EffigyRepositoryTransformer, 'Transforming repository for: {}', entityClassNode.name

        EntityModel model = EntityModelRegistry.lookup(entityClassNode)
        trace EffigyRepositoryTransformer, 'Found entity model: {}', model

        if (implementsCrud) {
            try {
                injectCreateMethod repositoryClassNode, model
                injectRetrieveMethod repositoryClassNode, model
                injectRetrieveAllMethod repositoryClassNode, model
                injectUpdateMethod repositoryClassNode, model
                injectDeleteMethod repositoryClassNode, model
                injectDeleteAllMethod repositoryClassNode, model

            } catch (ex) {
                ex.printStackTrace()
            }
        }
    }

    private static void removeAbstract(ClassNode repositoryClassNode) {
        if (Modifier.isAbstract(repositoryClassNode.modifiers)) {
            repositoryClassNode.modifiers = Modifier.PUBLIC
        }
    }

    private static void injectJdbcTemplate(ClassNode repositoryClassNode) {
        FieldNode jdbcTemplateFieldNode = new FieldNode(
            'jdbcTemplate', Modifier.PRIVATE, new ClassNode(JdbcTemplate), repositoryClassNode, new EmptyExpression()
        )

        jdbcTemplateFieldNode.addAnnotation(new AnnotationNode(new ClassNode(Autowired)))

        repositoryClassNode.addField(jdbcTemplateFieldNode)
    }
}
