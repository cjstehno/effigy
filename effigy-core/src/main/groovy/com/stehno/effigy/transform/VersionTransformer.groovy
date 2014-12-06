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
import static com.stehno.effigy.transform.util.AnnotationUtils.extractFieldName
import static com.stehno.effigy.transform.util.TransformUtils.findSqlType
import static org.codehaus.groovy.ast.ClassHelper.Long_TYPE
import static org.codehaus.groovy.ast.ClassHelper.long_TYPE

import com.stehno.effigy.transform.model.EntityModelRegistry
import com.stehno.effigy.transform.model.VersionerPropertyModel
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Transformer used by the Version annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class VersionTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        FieldNode versionFieldNode = nodes[1] as FieldNode
        ClassNode entityClassNode = versionFieldNode.owner

        debug EffigyEntityTransformer, 'Visiting Field: {}.{}', entityClassNode.name, versionFieldNode.name

        verifyVersionProperty versionFieldNode


        EntityModelRegistry.lookup(entityClassNode).replaceProperty(new VersionerPropertyModel(
            columnName: extractFieldName(versionFieldNode),
            propertyName: versionFieldNode.name,
            type: versionFieldNode.type,
            columnType: findSqlType(versionFieldNode)
        ))
    }

    private static void verifyVersionProperty(final FieldNode fieldNode) {
        if (!(fieldNode.type in [Long_TYPE, long_TYPE])) {
            throw new Exception('Currently the Version annotation may only be used on long or java.lang.Long fields.')
        }
    }
}
