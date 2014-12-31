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
import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.transform.sql.SqlTemplate
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.transform.model.EntityModel.entityProperty
import static com.stehno.effigy.transform.util.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

/**
 * Abstract parent class for the Effigy CRUD method implementation annotation transformers.
 */
abstract class MethodImplementingTransformation implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotationNode = nodes[0] as AnnotationNode
        MethodNode methodNode = nodes[1] as MethodNode
        ClassNode repositoryNode = methodNode.declaringClass

        try {
            AnnotationNode repositoryAnnot = repositoryNode.getAnnotations(make(Repository))[0]
            if (repositoryAnnot) {
                ClassNode entityNode = extractClass(repositoryAnnot, 'forEntity')

                if (isValidReturnType(methodNode.returnType, entityNode)) {
                    implementMethod annotationNode, repositoryNode, entityNode, methodNode

                } else {
                    error(
                        getClass(),
                        'Return type for method ({}) is not valid for the provided annotation ({}).',
                        methodNode.name,
                        annotationNode.classNode.nameWithoutPackage
                    )
                    throw new EffigyTransformationException()
                }

            } else {
                error getClass(), 'Repository method annotations may only be applied to methods of an Effigy Repository class - ignoring.'
                throw new EffigyTransformationException()
            }

        } catch (EffigyTransformationException etex) {
            throw etex

        } catch (ex) {
            error(
                getClass(),
                'Unable to implement {} method ({}) for ({}): {}',
                annotationNode.classNode.nameWithoutPackage,
                methodNode.name,
                repositoryNode.name,
                ex.message
            )
            throw ex
        }
    }

    abstract protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode)

    abstract protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode)

    @SuppressWarnings('GroovyAssignabilityCheck')
    protected static List extractParameters(AnnotationNode annotationNode, ClassNode entityNode, MethodNode methodNode, boolean ignoreFirst = false) {
        def wheres = []
        def params = []

        SqlTemplate template = extractSqlTemplate(annotationNode)
        if (template) {
            wheres << template.sql(entityNode)
            params.addAll(template.variableNames().collect { vn -> varX(vn[1..-1]) })

        } else {
            parameters(methodNode.parameters, ignoreFirst).findAll { p -> !p.getAnnotations(make(Limit)) }.each { mp ->
                wheres << "${entityProperty(entityNode, mp.name).columnName}=?"
                params << varX(mp.name)
            }
        }

        [wheres, params]
    }

    private static List parameters(Parameter[] params, boolean ignoreFirst) {
        if (ignoreFirst) {
            (params as List).tail()
        } else {
            params
        }
    }

    private static SqlTemplate extractSqlTemplate(final AnnotationNode node) {
        String value = extractString(node, 'value')
        value ? new SqlTemplate(value) : null
    }
}
