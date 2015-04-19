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

import java.sql.Types

import static com.stehno.effigy.transform.model.ColumnModelType.*
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.*
import static com.stehno.effigy.transform.util.StringUtils.camelCaseToUnderscore
import static java.lang.Integer.MIN_VALUE
import static org.codehaus.groovy.ast.ClassHelper.*

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
            extractColumnModel(idFieldNode)
        ) : null
    }

    static ColumnPropertyModel versioner(ClassNode entityNode) {
        FieldNode versionFieldNode = entityNode.fields.find { annotatedWith(it, Version) }

        versionFieldNode ? new ColumnPropertyModel(
            VERSION,
            versionFieldNode.name,
            versionFieldNode.type,
            extractColumnModel(versionFieldNode)
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
            !f.static && !f.name.startsWith(DOLLAR_SIGN) && !isAssociation(f) && !isEntity(f.type) && (includeId ? true : !annotatedWith(f, Id))
        }.collect { f -> extractProperty(f) }
    }

    private static EntityPropertyModel extractProperty(FieldNode f) {
        if (!f) {
            null

        } else if (annotatedWith(f, Id)) {
            new ColumnPropertyModel(ID, f.name, f.type, extractColumnModel(f))

        } else if (annotatedWith(f, Version)) {
            new ColumnPropertyModel(VERSION, f.name, f.type, extractColumnModel(f))

        } else if (annotatedWith(f, Embedded)) {
            extractEmbeddedProperty(f)

        } else {
            new ColumnPropertyModel(STANDARD, f.name, f.type, extractColumnModel(f))
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

    static List<Integer> columnTypes(ClassNode entityNode, boolean includeId = true) {
        def values = []
        entityProperties(entityNode, includeId).each {
            if (it instanceof EmbeddedPropertyModel) {
                values.addAll(it.columnTypes)
            } else {
                values << it.column.type
            }
        }
        values
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

                ColumnModel columnModel = extractColumnModel(embfld)
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

    @SuppressWarnings('GroovyAssignabilityCheck')
    private static ColumnModel extractColumnModel(FieldNode fieldNode) {
        AnnotationNode fieldColumnAnnot = fieldNode.getAnnotations(make(Column))[0]

        if (fieldColumnAnnot) {
            // FIXME: #16 this will be a PropertyExpression when using Types.XXX rather than int value
            int typeValue = extractInteger(fieldColumnAnnot, 'type', MIN_VALUE)
            ClassNode handlerNode = extractClass(fieldColumnAnnot, 'handler')

            return new ColumnModel(
                extractString(fieldColumnAnnot, 'value') ?: camelCaseToUnderscore(fieldNode.name),
                typeValue != MIN_VALUE ? typeValue : resolveDefaultSqlType(fieldNode),
                handlerNode == VOID_TYPE ? null : handlerNode
            )
        }

        return new ColumnModel(
            camelCaseToUnderscore(fieldNode.name),
            resolveDefaultSqlType(fieldNode),
            null
        )
    }

    private static int resolveDefaultSqlType(final FieldNode fieldNode) {
        if (fieldNode.type.enum) {
            return Types.VARCHAR
        }

        switch (fieldNode.type.name) {
            case 'java.lang.String': return Types.VARCHAR
            case 'java.sql.Date':
            case 'java.util.Date':
                return Types.TIMESTAMP
            case 'java.lang.Boolean':
            case 'boolean':
                return Types.BOOLEAN
            case 'java.lang.Integer':
            case 'int':
                return Types.INTEGER
            case 'java.lang.Long':
            case 'long':
                return Types.BIGINT
            case 'java.lang.Double':
            case 'double':
                return Types.DOUBLE
            case 'java.lang.Float':
            case 'float':
                return Types.FLOAT
            case 'java.lang.Short':
            case 'short':
                return Types.TINYINT
            default: return Types.JAVA_OBJECT
        }
    }
}
