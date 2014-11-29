package com.stehno.effigy.transform

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassNode

/**
 * Used by the AST transforms to help manage the entity metadata.
 */
@CompileStatic
interface EntityPropertyModel {

    String getPropertyName()
    ClassNode getType()
}

@Immutable(knownImmutableClasses = [ClassNode]) @CompileStatic
class IdentifierPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type
    String columnName
    int columnType
}

@Immutable(knownImmutableClasses = [ClassNode]) @CompileStatic
class VersionerPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type
    String columnName
    int columnType
}

@Immutable(knownImmutableClasses = [ClassNode]) @CompileStatic
class FieldPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type
    String columnName
    int columnType
}

@Immutable(knownImmutableClasses = [ClassNode]) @CompileStatic
class OneToManyPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type

    String table
    String entityId
    String associationId
}
