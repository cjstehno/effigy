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

import static com.stehno.effigy.logging.Logger.debug
import static com.stehno.effigy.logging.Logger.warn
import static com.stehno.effigy.transform.model.EntityModelRegistry.lookup
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static com.stehno.effigy.transform.util.TransformUtils.isEffigyEntity

import com.stehno.effigy.transform.model.OneToManyPropertyModel
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Created by cjstehno on 12/6/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class OneToManyTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotationNode = nodes[0] as AnnotationNode
        FieldNode o2MFieldNode = nodes[1] as FieldNode
        ClassNode entityClassNode = o2MFieldNode.owner

        if (isEffigyEntity(entityClassNode)) {
            debug OneToManyTransformer, 'Visiting Field: {}.{}', entityClassNode.name, o2MFieldNode.name

            String table = extractString(annotationNode, 'table')
            String entityId = extractString(annotationNode, 'entityId')
            String associationId = extractString(annotationNode, 'associationId')

            lookup(entityClassNode).replaceProperty(new OneToManyPropertyModel(
                propertyName: o2MFieldNode.name,
                type: o2MFieldNode.type,
                associatedType: o2MFieldNode.type.genericsTypes.find { isEffigyEntity(it.type) }.type,
                table: table,
                entityId: entityId,
                associationId: associationId
            ))

        } else {
            warn OneToManyTransformer, 'Annotated id field is not contained within an entity annotated with EffigyEntity - ignored.'
        }
    }
}