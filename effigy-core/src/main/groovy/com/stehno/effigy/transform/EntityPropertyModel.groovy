package com.stehno.effigy.transform

import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassNode

/**
 * Created by cjstehno on 11/27/2014.
 */
@Immutable(knownImmutableClasses = [ClassNode])
class EntityPropertyModel {

    boolean identifier
    boolean versioner
    boolean enumeration
    String propertyName
    ClassNode type
    String columnName
}
