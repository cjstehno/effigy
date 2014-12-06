/*
 * Copyright (c) 2014 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stehno.effigy.transform.model

import static com.stehno.effigy.logging.Logger.debug
import static com.stehno.effigy.transform.util.AnnotationUtils.*
import static com.stehno.effigy.transform.util.TransformUtils.findSqlType
import static org.codehaus.groovy.ast.ClassHelper.make

import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.OneToMany
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode

/**
 * General meta information used by the framework during compilation of entities.
 */
class EntityModel {

    ClassNode type
    String table
    Map<String, EntityPropertyModel> entityProperties

    EntityPropertyModel getIdentifier() {
        entityProperties.values().find { it instanceof IdentifierPropertyModel }
    }

    EntityPropertyModel getVersioner() {
        entityProperties.values().find { it instanceof VersionerPropertyModel }
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

    List<EntityPropertyModel> findProperties(final boolean includeId = true) {
        entityProperties.values().findAll {
            includeId ? !instanceOf(it, OneToManyPropertyModel) : !instanceOf(it, IdentifierPropertyModel, OneToManyPropertyModel)
        }
    }

    void replaceProperty(EntityPropertyModel propertyModel) {
        entityProperties[propertyModel.propertyName] = propertyModel
    }

    public <T> List<T> findPropertiesByType(Class<T> propModelType) {
        entityProperties.values().findAll { propModelType.isAssignableFrom(it.class) }
    }

    boolean hasAssociations() {
        entityProperties.values().count { it instanceof OneToManyPropertyModel } // TODO: will be more here
    }

    List<EntityPropertyModel> findAssociationProperties() {
        findPropertiesByType(OneToManyPropertyModel) // TODO: more will be added
    }

    // TODO: this should probably move to the registry class (?)
    static EntityModel registerEntityModel(final ClassNode entityClassNode) {
        def map = [:]

        entityClassNode.fields.findAll { f -> !f.static }.each { FieldNode field ->
            EntityPropertyModel propertyModel

            if (hasAnnotation(field, OneToMany)) {
                propertyModel = new OneToManyPropertyModel(
                    propertyName: field.name,
                    type: field.type,
                    associatedType: field.type.genericsTypes.find { hasAnnotation(it.type, EffigyEntity) }.type,
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

            map[propertyModel.propertyName] = propertyModel
        }


        EntityModelRegistry.instance.register(
            new EntityModel(
                table: extractTableName(entityClassNode),
                type: entityClassNode,
                entityProperties: map
            )
        )
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
