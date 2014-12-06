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
import static com.stehno.effigy.transform.util.AnnotationUtils.extractFieldName
import static com.stehno.effigy.transform.util.TransformUtils.findSqlType
import static com.stehno.effigy.transform.util.TransformUtils.isEffigyEntity

import com.stehno.effigy.transform.model.IdentifierPropertyModel
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Transformation support for the Effigy Id annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class IdTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        FieldNode idFieldNode = nodes[1] as FieldNode
        ClassNode entityClassNode = idFieldNode.owner

        if (isEffigyEntity(entityClassNode)) {
            debug IdTransformer, 'Visiting Field: {}.{}', entityClassNode.name, idFieldNode.name

            lookup(entityClassNode).replaceProperty(new IdentifierPropertyModel(
                columnName: extractFieldName(idFieldNode),
                propertyName: idFieldNode.name,
                type: idFieldNode.type,
                columnType: findSqlType(idFieldNode)
            ))

        } else {
            warn IdTransformer, 'Annotated id field is not contained within an entity annotated with EffigyEntity - ignored.'
        }
    }
}
