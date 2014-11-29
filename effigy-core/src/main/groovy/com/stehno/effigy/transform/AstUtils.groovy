package com.stehno.effigy.transform

import groovy.text.GStringTemplateEngine
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase

/**
 * Created by cjstehno on 11/28/2014.
 */
class AstUtils {

    static List<ASTNode> code(Map bindings=[:], String text) {
        new AstBuilder().buildFromString(
            CompilePhase.CANONICALIZATION,
            true,
            string(bindings, text)
        )
    }

    static String string(Map bindings=[:], String text){
        String result = new GStringTemplateEngine().createTemplate(text).make(bindings) as String
        println '--------------------------------'
        println result
        println '--------------------------------'
        result
    }

    static Statement codeS(Map bindings=[:], String text){
        code(bindings, text)[0] as Statement
    }

    static ArrayExpression arrayX(ClassNode type, List<Expression> expressions){
        new ArrayExpression(type, expressions)
    }
}
