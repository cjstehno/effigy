package com.stehno.effigy.transform
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import static com.stehno.effigy.transform.SqlHelperAnnotation.helperFrom
import static java.lang.reflect.Modifier.PRIVATE
import static java.lang.reflect.Modifier.PUBLIC
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
/**
 * FIXME: document me
 */
class ClassManipulationUtils {

    static boolean hasField(ClassNode classNode, String fieldName) {
        classNode.fields.find { f -> f.name == fieldName }
    }

    static FieldNode addSafeField(ClassNode classNode, Class type, String name, Expression initializer) {
        def node = new FieldNode(name, PRIVATE, makeClassSafe(type), classNode, initializer)
        classNode.addField(node)
        node
    }

    static void addSafeFieldIfMissing(ClassNode classNode, Class type, String name, Expression initializer) {
        if (!hasField(classNode, name)) {
            addSafeField classNode, type, name, initializer
        }
    }

    static void addFieldProperty(ClassNode classNode, FieldNode fieldNode) {
        classNode.addProperty(new PropertyNode(fieldNode, PUBLIC, null, null))
    }

    static void autowireField(FieldNode fieldNode, String beanName = null) {
        fieldNode.addAnnotation(new AnnotationNode(make(Autowired)))

        if (beanName) {
            def annotNode = new AnnotationNode(make(Qualifier))
            annotNode.setMember('value', constX(beanName))
            fieldNode.addAnnotation(annotNode)
        }
    }

    // FIXME: code below here does not belong in this class, but is shared, so should live somewhere

    private static final String SET_METHOD_ARGUMENTS = 'setMethodArguments'

    static Expression applyArguments(MethodNode methodNode, Class helperType, BlockStatement code, Expression generator) {
        if (helperFrom(methodNode, helperType)?.arguments) {
            String varName = "__${helperType.simpleName}"

            // need to replace the generator expression with a variable
            code.addStatement(declS(varX(varName), generator))
            generator = varX(varName)

            code.addStatement(stmt(callX(varX(varName), SET_METHOD_ARGUMENTS, new MapExpression(methodNode.parameters.collect { p ->
                new MapEntryExpression(constX(p.name), varX(p.name))
            }))))
        }
        generator
    }
}
