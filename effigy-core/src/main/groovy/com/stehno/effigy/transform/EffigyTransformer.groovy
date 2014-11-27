package com.stehno.effigy.transform

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Created by cjstehno on 11/26/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        println "Effigy: ${nodes[0]}"
    }
}
