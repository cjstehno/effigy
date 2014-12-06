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

import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AnnotationUtils.extractString
import static org.codehaus.groovy.ast.ClassHelper.make

import com.stehno.effigy.annotation.*
import com.stehno.effigy.transform.util.StringUtils
import org.codehaus.groovy.ast.*

import java.sql.Types

/**
 * Utility functions for working with the Effigy entity model.
 */
class EntityModel {

    static IdentifierPropertyModel identifier(ClassNode entityNode) {
        FieldNode idFieldNode = entityNode.fields.find { annotatedWith(it, Id) }

        new IdentifierPropertyModel(
            columnName: extractFieldName(idFieldNode),
            propertyName: idFieldNode.name,
            type: idFieldNode.type,
            columnType: sqlType(idFieldNode)
        )
    }

    static VersionerPropertyModel versioner(ClassNode entityNode) {
        FieldNode versionFieldNode = entityNode.fields.find { annotatedWith(it, Version) }

        return versionFieldNode ? new VersionerPropertyModel(
            columnName: extractFieldName(versionFieldNode),
            propertyName: versionFieldNode.name,
            type: versionFieldNode.type,
            columnType: sqlType(versionFieldNode)
        ) : null
    }

    // does not include relationship fields
    static List<EntityPropertyModel> entityProperties(ClassNode entityNode, boolean includeId = true) {
        entityNode.fields.findAll { f ->
            !f.static && !annotatedWith(f, OneToMany) && (includeId ? true : !annotatedWith(f, Id))
        }.collect { f ->
            if (annotatedWith(f, Id)) {
                new IdentifierPropertyModel(
                    columnName: extractFieldName(f),
                    propertyName: f.name,
                    type: f.type,
                    columnType: sqlType(f)
                )

            } else if (annotatedWith(f, Version)) {
                new VersionerPropertyModel(
                    columnName: extractFieldName(f),
                    propertyName: f.name,
                    type: f.type,
                    columnType: sqlType(f)
                )

            } else {
                new FieldPropertyModel(
                    columnName: extractFieldName(f),
                    propertyName: f.name,
                    type: f.type,
                    columnType: sqlType(f)
                )
            }
        }
    }

    static boolean hasAssociatedEntities(ClassNode entityNode) {
        entityNode.fields.find { f -> annotatedWith(f, OneToMany) }
    }

    static String entityTable(ClassNode entityNode) {
        AnnotationNode effigyAnnotNode = entityNode.getAnnotations(make(EffigyEntity))[0]
        extractString(effigyAnnotNode, 'table', entityNode.nameWithoutPackage.toLowerCase() + 's')
    }

    static List<OneToManyPropertyModel> oneToManyAssociations(ClassNode entityNode) {
        entityNode.fields.findAll { f -> annotatedWith(f, OneToMany) }.collect { o2mf ->
            def annotationNode = o2mf.getAnnotations(make(OneToMany))[0]
            def associatedType = o2mf.type.genericsTypes.find { isEffigyEntity(it.type) }.type

            String entityTableName = entityTable(entityNode)
            String assocTableName = entityTable(associatedType)

            String table = extractString(annotationNode, 'table', "${entityTableName}_${o2mf.name}")
            String entityId = extractString(annotationNode, 'entityId', "${entityTableName}_id")
            String associationId = extractString(annotationNode, 'associationId', "${assocTableName}_id")

            new OneToManyPropertyModel(
                propertyName: o2mf.name,
                type: o2mf.type,
                associatedType: associatedType,
                table: table,
                entityId: entityId,
                associationId: associationId
            )
        }
    }

    private static boolean annotatedWith(AnnotatedNode node, Class annotClass) {
        node.getAnnotations(make(annotClass))
    }

    private static String extractFieldName(final FieldNode field) {
        AnnotationNode fieldColumnAnnot = field.getAnnotations(ClassHelper.make(Column))[0]
        if (fieldColumnAnnot) {
            return extractString(fieldColumnAnnot, 'value')

        } else {
            return StringUtils.camelCaseToUnderscore(field.name)
        }
    }

    private static int sqlType(final FieldNode fieldNode) {
        if (fieldNode.type.enum) return Types.VARCHAR

        switch (fieldNode.type.name) {
            case 'java.lang.String': return Types.VARCHAR
            case 'java.sql.Date':
            case 'java.util.Date':
                return Types.TIMESTAMP
            case 'java.lang.Boolean':
            case 'boolean':
                return Types.BOOLEAN
            case 'java.lang.Integer': return Types.INTEGER
            case 'java.lang.Long':
            case 'long':
                return Types.BIGINT
            default: return Types.JAVA_OBJECT
        }
    }

    /**
     * Determines whether or not the specified ClassNode is annotated with the EffigyEntity annotation.
     *
     * @param node the class node being tested
     * @return true , if the class is annotated with EffigyEntity
     */
    static boolean isEffigyEntity(final ClassNode node) {
        node.getAnnotations(make(EffigyEntity))
    }
}
