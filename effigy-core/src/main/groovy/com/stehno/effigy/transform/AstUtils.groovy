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
            new GStringTemplateEngine().createTemplate(text).make(bindings) as String
        )
    }

    static Statement codeS(Map bindings=[:], String text){
        code(bindings, text)[0] as Statement
    }

    static Expression codeX(Map bindings=[:], String text){
        code(bindings, text)[0] as Expression
    }

    static ArrayExpression arrayX(ClassNode type, List<Expression> expressions){
        new ArrayExpression(type, expressions)
    }
}
