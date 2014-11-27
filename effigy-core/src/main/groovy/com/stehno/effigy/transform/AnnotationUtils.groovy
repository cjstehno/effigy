package com.stehno.effigy.transform

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression

/**
 * Created by cjstehno on 11/26/2014.
 */
class AnnotationUtils {

    public static ClassNode extractClass(AnnotationNode annotation, String key) {
        def pair = annotation.members.find { pair -> pair.key == key; };
        if (pair == null) return null;

        assert (pair.value instanceof ClassExpression)
        return pair.value.type;
    }

    public static String extractString(AnnotationNode annotation, String key) {
        def pair = annotation.members.find { pair -> pair.key == key; };
        if (pair) {
            return pair.value.value;
        } else {
            return null;
        }
    }
}
