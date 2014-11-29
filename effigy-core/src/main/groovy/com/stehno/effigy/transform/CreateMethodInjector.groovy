package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AnnotationUtils.hasAnnotation
import static com.stehno.effigy.transform.AstUtils.*
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import java.lang.reflect.Modifier

/**
 * Injects the code for the repository entity create method.
 */
class CreateMethodInjector {

    static void injectCreateMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        try {
            model.findPropertiesByType(OneToManyPropertyModel).each { OneToManyPropertyModel o2m ->
                injectO2MSaveMethod(repositoryClassNode, model, o2m)
            }

            def statement = block(
                declS(varX('keys'), ctorX(make(GeneratedKeyHolder))),

                model.versioner ? codeS('entity.$name = 0', name: model.versioner.propertyName) : new EmptyStatement(),

                declS(varX('factory'), ctorX(make(PreparedStatementCreatorFactory), args(
                    constX("insert into ${model.table} (${model.columnNames(false).join(',')}) values (${model.columnPlaceholders(false).join(',')})" as String),
                    arrayX(ClassHelper.int_TYPE, model.columnTypes(false).collect { typ ->
                        constX(typ)
                    })
                ))),

                codeS(
                    '''
                        def paramValues = [$values] as Object[]
                        jdbcTemplate.update(factory.newPreparedStatementCreator(paramValues), keys)
                        entity.${idName} = keys.key

                        $o2m

                        return keys.key
                    ''',

                    values: model.findProperties(false).collect { pi ->
                        if (pi.type.enum) {
                            "entity.${pi.propertyName}?.name()"
                        } else {
                            "entity.${pi.propertyName}"
                        }
                    }.join(','),

                    idName: model.identifier.propertyName,
                    o2m: model.findPropertiesByType(OneToManyPropertyModel).collect { OneToManyPropertyModel o2m ->
                        "save${o2m.propertyName.capitalize()}(entity)"
                    }.join('\n')
                ),
            )

            repositoryClassNode.addMethod(new MethodNode(
                'create',
                Modifier.PUBLIC,
                model.identifier.type,
                [new Parameter(model.type, 'entity')] as Parameter[],
                null,
                statement
            ))

        } catch (ex) {
            ex.printStackTrace()
        }
    }

    // TODO: this should probably be pulled into a common area since update uses the same method
    private static void injectO2MSaveMethod(ClassNode repositoryClassNode, EntityModel model, OneToManyPropertyModel o2m) {
        def statement = codeS(
            '''
                int expects = entity.${name}.size()
                int count = 0
                def ent = entity
                entity.${name}.each { itm->
                    jdbcTemplate.update('delete from $assocTable where $tableEntIdName=?', ent.${entityIdName})

                    count += jdbcTemplate.update(
                        'insert into $assocTable ($tableEntIdName,$tableAssocIdName) values (?,?)',
                        ent.${entityIdName},
                        itm.${assocIdName}
                    )
                }

                if( count != expects ){
                    entity.${entityIdName} = 0
                    throw new RuntimeException('Insert count for $name (' + count + ') did not match expected count (' + expects + ') - save failed.')
                }
            ''',

            name: o2m.propertyName,
            assocTable: o2m.table,
            tableEntIdName: o2m.entityId,
            tableAssocIdName: o2m.associationId,
            entityIdName: model.identifier.propertyName,
            assocIdName: findIdName(o2m.type)
        )

        repositoryClassNode.addMethod(new MethodNode(
            "save${o2m.propertyName.capitalize()}",
            Modifier.PROTECTED,
            ClassHelper.VOID_TYPE,
            [new Parameter(model.type, 'entity')] as Parameter[],
            null,
            statement
        ))
    }

    // FIXME: this should be part of the model (?)
    private static findIdName(ClassNode classNode) {
        GenericsType mappedType = classNode.genericsTypes.find { hasAnnotation(it.type, EffigyEntity) }
        mappedType.type.fields.find { hasAnnotation(it, Id) }.name
    }
}