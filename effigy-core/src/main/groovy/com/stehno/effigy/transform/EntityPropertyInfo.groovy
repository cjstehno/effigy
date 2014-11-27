package com.stehno.effigy.transform

import groovy.transform.ToString
import org.codehaus.groovy.ast.ClassNode

/**
 * Created by cjstehno on 11/27/2014.
 */
@ToString(includeNames = true)
class EntityPropertyInfo {
    boolean id
    String fieldName
    String propertyName
    ClassNode type
}
