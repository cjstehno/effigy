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

import static com.stehno.effigy.transform.model.EntityModel.registerEntityModel
import static org.codehaus.groovy.ast.ClassHelper.Long_TYPE
import static org.codehaus.groovy.ast.ClassHelper.long_TYPE

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Handles the transformation of the Effigy Entity classes (annotated with the EffigyEntity annotation).
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyEntityTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode

        verifyVersionProperty(entityClassNode)

        registerEntityModel entityClassNode
    }

    private static void verifyVersionProperty(final ClassNode entityClassNode) {
        FieldNode versionProperty = entityClassNode.fields.find { FieldNode f ->
            f.annotations.find { AnnotationNode a -> a.classNode.name == 'com.stehno.effigy.annotation.Version' }
        }

        if (versionProperty && !(versionProperty.type in [Long_TYPE, long_TYPE])) {
            throw new Exception('Currently the Version annotation may only be used on long or java.lang.Long fields.')
        }
    }
}
