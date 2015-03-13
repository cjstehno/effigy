package com.stehno.effigy.transform

import com.stehno.effigy.transform.util.AnnotationUtils
import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode

/**
 * Immutable object stub used to represent the Sql helper annotation values during the transformation.
 */
@Immutable(knownImmutableClasses = [ClassNode])
class SqlHelperAnnotation {

    private static final String BEAN = 'bean'
    private static final String TYPE = 'type'
    private static final String FACTORY = 'factory'
    private static final String DEFAULT_EMPTY = ''

    String bean
    ClassNode type
    String factory
    boolean singleton
    boolean arguments

    static SqlHelperAnnotation helperFrom(MethodNode methodNode, Class annotType) {
        def annot = methodNode.getAnnotations(ClassHelper.make(annotType))[0]

        if (annot) {
            boolean args = AnnotationUtils.extractBoolean(annot, 'arguments', false)
            ClassNode helperNode = AnnotationUtils.extractClass(annot, TYPE)

            return new SqlHelperAnnotation(
                AnnotationUtils.extractString(annot, BEAN, DEFAULT_EMPTY),
                helperNode != ClassHelper.VOID_TYPE ? helperNode : null,
                AnnotationUtils.extractString(annot, FACTORY, DEFAULT_EMPTY),
                args ? false : AnnotationUtils.extractBoolean(annot, 'singleton', true),
                args
            )
        }

        return null
    }
}
