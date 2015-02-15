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

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.lang.reflect.Modifier

import static com.stehno.effigy.logging.Logger.*
import static com.stehno.effigy.transform.util.AnnotationUtils.*
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.getAllMethods

/**
 * Transformer used for processing the EffigyRepository annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class RepositoryTransformer implements ASTTransformation {

    private static final Map<Class, List<RepositoryMethodVisitor>> TRANSFORMERS = [
        Create  : [new AssociationSaveMethodInjector(), new CreateTransformer()],
        Retrieve: [new RetrieveTransformer()],
        Update  : [new AssociationSaveMethodInjector(), new UpdateTransformer()],
        Delete  : [new DeleteTransformer()],
        Count   : [new CountTransformer()],
        Exists  : [new ExistsTransformer()]
    ]

    private static final String JDBC_TEMPLATE = 'jdbcTemplate'
    private static final String TEMPLATE = 'template'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        try {
            ClassNode repoNode = nodes[1] as ClassNode

            AnnotationNode repoAnnotation = repoNode.getAnnotations(make(com.stehno.effigy.annotation.Repository))[0]
            ClassNode entityNode = extractClass(repoAnnotation, 'value')

            removeAbstract repoNode
            applyRepositoryAnnotation repoNode
            injectJdbcTemplate repoNode, repoAnnotation

            getAllMethods(repoNode).each { method ->
                AnnotationNode annot = method.annotations.find { a ->
                    a.classNode.nameWithoutPackage in ['Create', 'Retrieve', 'Update', 'Delete', 'Count', 'Exists']
                }

                if (annot) {
                    trace(
                        RepositoryTransformer,
                        'Found CRUD annotation ({}) on method ({}) of repository ({}).',
                        annot.classNode.nameWithoutPackage,
                        method.name,
                        repoNode.name
                    )

                    TRANSFORMERS[annot.classNode.nameWithoutPackage]*.visit(repoNode, entityNode, annot, method)
                }
            }

        } catch (ex) {
            warn RepositoryTransformer, 'Problem transforming repository: {}', ex.message
        }
    }

    private static void removeAbstract(ClassNode repositoryClassNode) {
        if (Modifier.isAbstract(repositoryClassNode.modifiers)) {
            repositoryClassNode.modifiers = PUBLIC
            info RepositoryTransformer, 'Removed abstract from repository class ({}).', repositoryClassNode.name
        }
    }

    /**
     * Apply the Spring Repository annotation so that the Effigy Repository is essentially an extension of it.
     */
    private static void applyRepositoryAnnotation(ClassNode repositoryNode) {
        if (!hasAnnotation(repositoryNode, Repository)) {
            repositoryNode.addAnnotation(new AnnotationNode(make(Repository)))
        }
    }

    private static void injectJdbcTemplate(ClassNode repositoryNode, AnnotationNode annotationNode) {
        PropertyNode jdbcTemplateNode = new PropertyNode(
            JDBC_TEMPLATE, PUBLIC, make(JdbcTemplate), repositoryNode, new EmptyExpression(), null, null
        )

        if (extractBoolean(annotationNode, 'autowired')) {
            jdbcTemplateNode.addAnnotation(new AnnotationNode(new ClassNode(Autowired)))

            String qualifier = extractString(annotationNode, 'qualifier', '')
            if (qualifier) {
                def qualifierAnnot = new AnnotationNode(make(Qualifier))
                qualifierAnnot.setMember('value', constX(qualifier))

                jdbcTemplateNode.addAnnotation(qualifierAnnot)
            }
        }

        repositoryNode.addProperty(jdbcTemplateNode)

        info RepositoryTransformer, 'Added autowired JdbcTemplate property to repository class ({}).', repositoryNode.name
    }
}
