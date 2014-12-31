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

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

import static com.stehno.effigy.transform.model.EntityModel.hasAssociatedEntities
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithAssociations
import static com.stehno.effigy.transform.sql.RetrievalSql.selectWithoutAssociations
import static com.stehno.effigy.transform.util.JdbcTemplateHelper.*
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

/**
 * Transformer used to process the @Retrieve annotations.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class RetrieveTransformer extends MethodImplementingTransformation {

    @Override
    protected boolean isValidReturnType(ClassNode returnType, ClassNode entityNode) {
        returnType.implementsInterface(makeClassSafe(Collection))
    }

    @Override
    protected void implementMethod(AnnotationNode annotationNode, ClassNode repoNode, ClassNode entityNode, MethodNode methodNode) {
        def code

        if (hasAssociatedEntities(entityNode)) {
            code = retrieveAllWithAssociations(entityNode)
        } else {
            code = retrieveAllWithoutAssociations(entityNode)
        }

        methodNode.modifiers = PUBLIC
        methodNode.code = code
    }

    private static Statement retrieveAllWithAssociations(ClassNode entityNode) {
        block(
            query(
                selectWithAssociations(entityNode),
                entityCollectionExtractor(entityNode)
            )
        )
    }

    private static Statement retrieveAllWithoutAssociations(ClassNode entityNode) {
        block(
            query(
                selectWithoutAssociations(entityNode),
                entityRowMapper(entityNode)
            )
        )
    }
}
