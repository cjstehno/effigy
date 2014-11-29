package com.stehno.effigy.transform

import static com.stehno.effigy.logging.Logger.debug
import static com.stehno.effigy.transform.AnnotationUtils.extractString
import static com.stehno.effigy.transform.AnnotationUtils.hasAnnotation
import static com.stehno.effigy.transform.StringUtils.camelCaseToUnderscore
import static java.sql.Types.*
import static org.codehaus.groovy.ast.ClassHelper.make

import com.stehno.effigy.annotation.*
import groovy.transform.Immutable
import groovy.transform.Memoized
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode

/**
 * General meta information used by the framework during compilation of entities.
 */
@Immutable(knownImmutableClasses = [ClassNode])
class EntityModel {

    ClassNode type
    String table
    List<EntityPropertyModel> entityProperties

    @Memoized
    EntityPropertyModel getIdentifier() {
        entityProperties.find { it instanceof IdentifierPropertyModel }
    }

    @Memoized
    EntityPropertyModel getVersioner() {
        entityProperties.find { it instanceof VersionerPropertyModel }
    }

    List<String> columnNames(final boolean includeId = true) {
        findProperties(includeId).collect { it.columnName }
    }

    List<String> columnPlaceholders(final boolean includeId = true) {
        findProperties(includeId).collect { '?' }
    }

    List<Integer> columnTypes(final boolean includeId = true) {
        findProperties(includeId).collect { it.columnType }
    }

    @Memoized
    List<EntityPropertyModel> findProperties(final boolean includeId = true) {
        entityProperties.findAll {
            includeId ? !instanceOf(it, OneToManyPropertyModel) : !instanceOf(it, IdentifierPropertyModel, OneToManyPropertyModel)
        }
    }

    public <T> List<T> findPropertiesByType(Class<T> propModelType) {
        entityProperties.findAll { propModelType.isAssignableFrom(it.class) }
    }

    static EntityModel extractEntityInfo(final ClassNode entityClassNode) {
        new EntityModel(
            table: extractTableName(entityClassNode),
            type: entityClassNode,
            entityProperties: entityClassNode.fields.findAll { f -> !f.static }.collect { FieldNode field ->
                EntityPropertyModel propertyModel

                if (hasAnnotation(field, Id)) {
                    propertyModel = new IdentifierPropertyModel(
                        columnName: extractFieldName(field),
                        propertyName: field.name,
                        type: field.type,
                        columnType: findSqlType(field)
                    )

                } else if (hasAnnotation(field, Version)) {
                    propertyModel = new VersionerPropertyModel(
                        columnName: extractFieldName(field),
                        propertyName: field.name,
                        type: field.type,
                        columnType: findSqlType(field)
                    )

                } else if (hasAnnotation(field, OneToMany)) {
                    propertyModel = new OneToManyPropertyModel(
                        propertyName: field.name,
                        type: field.type,
                        table: extractString(field.getAnnotations(make(OneToMany))[0], 'table'),
                        entityId: extractString(field.getAnnotations(make(OneToMany))[0], 'entityId'),
                        associationId: extractString(field.getAnnotations(make(OneToMany))[0], 'associationId')
                    )

                } else {
                    propertyModel = new FieldPropertyModel(
                        columnName: extractFieldName(field),
                        propertyName: field.name,
                        type: field.type,
                        columnType: findSqlType(field)
                    )
                }

                debug EntityModel, 'Extracted ({}.{}): {}', entityClassNode.nameWithoutPackage, field.name, propertyModel

                return propertyModel

            }.asImmutable()
        )
    }

    private static int findSqlType(final FieldNode fieldNode) {
        if (fieldNode.type.enum) return VARCHAR

        switch (fieldNode.type.name) {
            case 'java.lang.String': return VARCHAR
            case 'java.sql.Date':
            case 'java.util.Date':
                return TIMESTAMP
            case 'java.lang.Boolean':
            case 'boolean':
                return BOOLEAN
            case 'java.lang.Integer': return INTEGER
            case 'java.lang.Long':
            case 'long':
                return BIGINT
            default: return JAVA_OBJECT
        }
    }

    private static String extractFieldName(FieldNode field) {
        AnnotationNode fieldColumnAnnot = field.getAnnotations(make(Column))[0]
        if (fieldColumnAnnot) {
            return extractString(fieldColumnAnnot, 'value')

        } else {
            return camelCaseToUnderscore(field.name)
        }
    }

    private static String extractTableName(final ClassNode entityClassNode) {
        AnnotationNode effigyAnnotNode = entityClassNode.getAnnotations(make(EffigyEntity))[0]
        extractString(effigyAnnotNode, 'table') ?: (entityClassNode.nameWithoutPackage.toLowerCase() + 's')
    }

    /**
     * Tests the provided class to see if it is an instance of one of the provided test classes. Any match will return success.
     *
     * @param type
     * @param tests
     * @return
     */
    private static boolean instanceOf(EntityPropertyModel type, Class<? extends EntityPropertyModel>... tests) {
        tests.find { it.isAssignableFrom(type.class) }
    }
}
