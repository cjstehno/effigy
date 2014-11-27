package com.stehno.effigy.transform
import static com.stehno.effigy.transform.AnnotationUtils.extractString

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import groovy.transform.ToString
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode

import java.sql.Types
/**
 * Created by cjstehno on 11/27/2014.
 */
@ToString(includeNames = true)
class EntityInfo {
    ClassNode type
    String table
    List<EntityPropertyInfo> props = []

    ClassNode getIdType() {
        props.find { it.id }.type
    }

    String getIdPropertyName(){
        props.find { it.id }.propertyName
    }

    String fieldNamesString(boolean includeId) {
        propertyInfo(includeId).collect { it.fieldName }.join(',')
    }

    String placeholderString(boolean includeId) {
        propertyInfo(includeId).collect { '?' }.join(',')
    }

    List<EntityPropertyInfo> propertyInfo(boolean includeId) {
        (includeId ? props : props.findAll { !it.id })
    }

    List<Integer> types(boolean includeId) {
        propertyInfo(includeId).collect {
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

    static EntityInfo extractEntityInfo(final ClassNode entityClassNode) {
        AnnotationNode effigyAnnotNode = entityClassNode.getAnnotations(new ClassNode(EffigyEntity))[0]

        println "Entity has Effigy annotation: ${effigyAnnotNode != null}"

        String tableName = extractString(effigyAnnotNode, 'table')
        if (!tableName) {
            tableName = entityClassNode.nameWithoutPackage.toLowerCase() + 's'
        }

        // list fields
        EntityInfo entityInfo = new EntityInfo(
            table: tableName,
            type: entityClassNode
        )

        entityClassNode.fields.each { FieldNode field ->
            String fieldName

            AnnotationNode fieldColumnAnnot = field.getAnnotations(new ClassNode(Column))[0]
            if (fieldColumnAnnot) {
                fieldName = extractString(fieldColumnAnnot, 'value')

            } else {
                fieldName = StringUtils.camelCaseToUnderscore(field.name)
            }

            def idAnnot = field.getAnnotations(new ClassNode(Id))[0]

            entityInfo.props << new EntityPropertyInfo(
                id: idAnnot != null,
                fieldName: fieldName,
                propertyName: field.name,
                type: field.type
            )
        }

        entityInfo
    }
}
