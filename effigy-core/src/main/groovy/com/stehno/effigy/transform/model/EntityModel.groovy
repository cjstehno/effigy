/*
 * Copyright (c) 2015 Christopher J. Stehno
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

import com.stehno.effigy.annotation.*
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode

import static com.stehno.effigy.transform.model.ColumnModel.extract
import static com.stehno.effigy.transform.model.ColumnModelType.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static org.codehaus.groovy.ast.ClassHelper.MAP_TYPE
import static org.codehaus.groovy.ast.ClassHelper.make

/**
 * Utility functions for working with the Effigy entity model.
 */
@Slf4j
class EntityModel {

    private static final String PLURAL = 's'
    private static final String DOLLAR_SIGN = '$'
    private static final String COMMA = ','
    private static final String ENTITY_COLUMN = 'entityColumn'

    static ColumnPropertyModel identifier(ClassNode entityNode) {
        FieldNode idFieldNode = entityNode.fields.find { annotatedWith(it, Id) }

        idFieldNode ? new ColumnPropertyModel(
            ID,
            idFieldNode.name,
            idFieldNode.type,
            extract(idFieldNode)
        ) : null
    }

    static ColumnPropertyModel versioner(ClassNode entityNode) {
        FieldNode versionFieldNode = entityNode.fields.find { annotatedWith(it, Version) }

        versionFieldNode ? new ColumnPropertyModel(
            VERSION,
            versionFieldNode.name,
            versionFieldNode.type,
            extract(versionFieldNode)
        ) : null
    }

    static EntityPropertyModel entityProperty(ClassNode entityNode, String propertyName) {
        extractProperty(entityNode.fields.find { f ->
            !f.static && f.name == propertyName && !isAssociation(f) && !isEntity(f.type)
        })
    }

    // does not include relationship fields
    static List<EntityPropertyModel> entityProperties(ClassNode entityNode, boolean includeId = true) {
        entityNode.fields.findAll { f ->
            isEntityProperty(f) && (includeId ? true : !annotatedWith(f, Id))
        }.collect { f -> extractProperty(f) }
    }

    private static boolean isEntityProperty(FieldNode f) {
        !f.static &&
            !f.name.startsWith(DOLLAR_SIGN) &&
            !isAssociation(f) &&
            !isEntity(f.type) &&
            !annotatedWith(f, Transient)
    }

    private static EntityPropertyModel extractProperty(FieldNode f) {
        if (!f || annotatedWith(f, Transient)) {
            null

        } else if (annotatedWith(f, Id)) {
            new ColumnPropertyModel(ID, f.name, f.type, extract(f))

        } else if (annotatedWith(f, Version)) {
            new ColumnPropertyModel(VERSION, f.name, f.type, extract(f))

        } else if (annotatedWith(f, Embedded)) {
            extractEmbeddedProperty(f)

        } else {
            new ColumnPropertyModel(STANDARD, f.name, f.type, extract(f))
        }
    }

    static boolean hasAssociatedEntities(ClassNode entityNode) {
        entityNode.fields.find { f -> isAssociation(f) || isEntity(f.type) }
    }

    static String entityTable(ClassNode entityNode) {
        AnnotationNode effigyAnnotNode = entityNode.getAnnotations(make(Entity))[0]
        if (effigyAnnotNode) {
            return extractString(effigyAnnotNode, 'table', entityNode.nameWithoutPackage.toLowerCase() + PLURAL)
        }
        return entityNode.nameWithoutPackage.toLowerCase() + PLURAL
    }

    static List<EmbeddedPropertyModel> embeddedEntityProperties(ClassNode entityNode) {
        entityNode.fields.findAll { f -> annotatedWith(f, Embedded) }.collect { field ->
            extractEmbeddedProperty(field)
        }
    }

    static List<AssociationPropertyModel> associations(ClassNode entityNode) {
        entityNode.fields.findAll { f -> annotatedWith(f, Association) || isEntity(f.type) }.collect { assoc ->
            def annotationNode = assoc.getAnnotations(make(Association))[0]
            if (annotationNode) {
                def associatedType = assoc.type.genericsTypes.find { isEntity(it.type) }.type

                String entityTableName = entityTable(entityNode)
                String assocTableName = entityTable(associatedType)

                // TODO: find a better way to do this
                String mapKey = null
                if (assoc.type.isDerivedFrom(MAP_TYPE)) {
                    def mappedAnno = assoc.getAnnotations(make(Mapped))[0]
                    if (mappedAnno) {
                        mapKey = extractString(mappedAnno, 'keyProperty')
                    } else {
                        mapKey = identifier(associatedType).propertyName
                    }
                }

                return new AssociationPropertyModel(
                    propertyName: assoc.name,
                    type: assoc.type,
                    associatedType: associatedType,
                    joinTable: extractString(annotationNode, 'joinTable', "${entityTableName}_${assoc.name}"),
                    entityColumn: extractString(annotationNode, ENTITY_COLUMN, "${entityTableName}_id"),
                    assocColumn: extractString(annotationNode, 'assocColumn', "${assocTableName}_id"),
                    mapKeyProperty: mapKey
                )
            }

            // handle naked entity type (1-1 association)
            String entityTableName = entityTable(entityNode)

            return new AssociationPropertyModel(
                propertyName: assoc.name,
                type: assoc.type,
                associatedType: assoc.type,
                joinTable: "${entityTableName}_${assoc.name}",
                entityColumn: "${entityTableName}_id",
                assocColumn: "${entityTable(assoc.type)}_id"
            )
        }
    }

    static List<ComponentPropertyModel> components(final ClassNode entityNode) {
        entityNode.fields.findAll { f -> annotatedWith(f, Component) }.collect { comp ->
            def annotationNode = comp.getAnnotations(make(Component))[0]

            new ComponentPropertyModel(
                propertyName: comp.name,
                type: comp.type,
                lookupTable: extractString(annotationNode, 'lookupTable', "${comp.name}s"),
                entityColumn: extractString(annotationNode, ENTITY_COLUMN, "${entityTable(entityNode)}_id")
            )
        }
    }

    // FIXME: seems like this should live in the SQL stuff (or somewhere else)
    static List<String> listColumnNames(ClassNode entityNode, boolean includeId = true) {
        def values = []
        entityProperties(entityNode, includeId).each {
            if (it instanceof EmbeddedPropertyModel) {
                values.addAll(it.columnNames)
            } else {
                values << it.column.name
            }
        }
        values
    }

    // FIXME: seems like this should live in the SQL stuff (or somewhere else)
    static String columnNames(ClassNode entityNode, boolean includeId = true) {
        listColumnNames(entityNode, includeId).join(COMMA)
    }

    // FIXME: seems like this should live in the SQL stuff (or somewhere else)
    static String columnPlaceholders(ClassNode entityNode, boolean includeId = true) {
        def values = []
        entityProperties(entityNode, includeId).each {
            if (it instanceof EmbeddedPropertyModel) {
                values.addAll(it.columnNames.collect { '?' })
            } else {
                values << '?'
            }
        }
        values.join(COMMA)
    }

    private static boolean annotatedWith(AnnotatedNode node, Class annotClass) {
        node.getAnnotations(make(annotClass))
    }

    /**
     * Determines whether or not the specified ClassNode is annotated with the EffigyEntity annotation.
     *
     * @param node the class node being tested
     * @return true , if the class is annotated with EffigyEntity
     */
    static boolean isEntity(final ClassNode node) {
        node.getAnnotations(make(Entity))
    }

    private static boolean isAssociation(FieldNode fieldNode) {
        annotatedWith(fieldNode, Association) || annotatedWith(fieldNode, Component)
    }

    private static EmbeddedPropertyModel extractEmbeddedProperty(FieldNode f) {
        String prefix = extractString(f.getAnnotations(make(Embedded))[0], 'prefix', f.name)

        def fldNames = []
        def colNames = []
        def colTypes = []

        f.type.fields.each { embfld ->
            if (!embfld.static && !embfld.name.startsWith(DOLLAR_SIGN)) {
                fldNames << embfld.name

                ColumnModel columnModel = extract(embfld)
                colNames << "${prefix}_${columnModel.name}"
                colTypes << columnModel.type
            }
        }

        new EmbeddedPropertyModel(
            propertyName: f.name,
            type: f.type,
            fieldNames: fldNames.asImmutable(),
            columnNames: colNames.asImmutable(),
            columnTypes: colTypes.asImmutable()
        )
    }
}
