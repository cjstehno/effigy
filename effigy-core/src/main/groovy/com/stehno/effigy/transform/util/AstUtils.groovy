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

package com.stehno.effigy.transform.util

import static com.stehno.effigy.logging.Logger.trace
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

import groovy.text.GStringTemplateEngine
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/28/2014.
 */
class AstUtils {

    static List<ASTNode> code(Map bindings = [:], String text) {
        new AstBuilder().buildFromString(
            CompilePhase.CANONICALIZATION,
            true,
            string(bindings, text)
        )
    }

    static String string(Map bindings = [:], String text) {
        String result = new GStringTemplateEngine().createTemplate(text).make(bindings) as String

        trace AstUtils, '-' * 50
        trace AstUtils, result
        trace AstUtils, '-' * 50

        result
    }

    static Statement codeS(Map bindings = [:], String text) {
        code(bindings, text)[0] as Statement
    }

    static ArrayExpression arrayX(ClassNode type, List<Expression> expressions) {
        new ArrayExpression(type, expressions)
    }

    static ClassNode classN(String name, Class returnType) {
        new ClassNode(
            name,
            Modifier.PUBLIC,
            makeClassSafe(returnType),
            [] as ClassNode[],
            [] as MixinNode[]
        )
    }

    static MethodNode methodN(
        int mod, String name, ClassNode returnType, Statement body, Parameter[] params = [] as Parameter[], ClassNode[] exceptions = [] as ClassNode[]
    ) {
        new MethodNode(name, mod, returnType, params, exceptions, body)
    }
}
