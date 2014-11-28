package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AnnotationUtils.extractString

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.annotation.Version
import groovy.transform.Memoized
import groovy.transform.ToString
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode

import java.sql.Types

/**
 * General meta information used by the framework during compilation of entities.
 */
@ToString(includeNames = true)
class EntityModel {
    // TODO: move entity info to the entity class (as static content - might be useful to outside code) ??

    ClassNode type
    String table
    List<EntityPropertyModel> entityProperties = []

    @Memoized
    EntityPropertyModel getIdentifier(){
        entityProperties.find { it.identifier }
    }

    @Memoized
    EntityPropertyModel getVersioner(){
        entityProperties.find { it.versioner }
    }

    List<EntityPropertyModel> findProperties(boolean includeId=true) {
        (includeId ? entityProperties : entityProperties.findAll { !it.identifier })
    }

    List<Integer> findSqlTypes(boolean includeId=true) {
        findProperties(includeId).collect {
            switch (it.type.nameWithoutPackage) {
                case 'String': return Types.VARCHAR
                case 'Date': return Types.TIMESTAMP
                case 'Boolean':
                case 'boolean':
                    return Types.BOOLEAN
                case 'Integer': return Types.INTEGER
                case 'Long':
                case 'long':
                    return Types.BIGINT
                default: return Types.JAVA_OBJECT
            }
        }
    }

    static EntityModel extractEntityInfo(final ClassNode entityClassNode) {
        AnnotationNode effigyAnnotNode = entityClassNode.getAnnotations(new ClassNode(EffigyEntity))[0]

        println "Entity has Effigy annotation: ${effigyAnnotNode != null}"

        String tableName = extractString(effigyAnnotNode, 'table')
        if (!tableName) {
            tableName = entityClassNode.nameWithoutPackage.toLowerCase() + 's'
        }

        // list fields
        EntityModel entityInfo = new EntityModel(
            table: tableName,
            type: entityClassNode
        )

        entityClassNode.fields.each { FieldNode field ->
            if( field.static ) return

            String fieldName

            AnnotationNode fieldColumnAnnot = field.getAnnotations(new ClassNode(Column))[0]
            if (fieldColumnAnnot) {
                fieldName = extractString(fieldColumnAnnot, 'value')

            } else {
                fieldName = StringUtils.camelCaseToUnderscore(field.name)
            }

            def idAnnot = field.getAnnotations(new ClassNode(Id))[0]
            def versionAnnot = field.getAnnotations(new ClassNode(Version))[0]

            entityInfo.entityProperties << new EntityPropertyModel(
                identifier: idAnnot != null,
                versioner: versionAnnot != null,
                enumeration: field.type.enum,
                columnName: fieldName,
                propertyName: field.name,
                type: field.type
            )
        }

        entityInfo
    }
}
