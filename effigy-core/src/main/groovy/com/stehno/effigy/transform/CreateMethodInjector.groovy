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

                        def ent = entity
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
                    o2m:model.findPropertiesByType(OneToManyPropertyModel).collect {OneToManyPropertyModel o2m->
                        genO2M(o2m, model.identifier)
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

    // FIXME: need custom exception for O2M check

    private static String genO2M(OneToManyPropertyModel model, IdentifierPropertyModel idModel){
        string(
            '''
                int ${name}_expects = entity.${name}.size()
                int ${name}_count = 0
                entity.${name}.each { itm->
                    ${name}_count += jdbcTemplate.update(
                        'insert into $assocTable ($tableEntIdName,$tableAssocIdName) values (?,?)',
                        ent.${entityIdName},
                        itm.${assocIdName}
                    )
                }

                if( ${name}_count != ${name}_expects ){
                    entity.${entityIdName} = 0
                    throw new Exception('Count did not match expected - update failed.')
                }

            ''',

            name: model.propertyName,
            assocTable:model.table,
            tableEntIdName:model.entityId,
            tableAssocIdName:model.associationId,
            entityIdName:idModel.propertyName,
            assocIdName:findIdName(model.type)
        )
    }

    // FIXME: this should be part of the model (?)
    private static findIdName( ClassNode classNode ){
        GenericsType mappedType = classNode.genericsTypes.find { hasAnnotation(it.type, EffigyEntity) }
        mappedType.type.fields.find { hasAnnotation(it, Id) }.name
    }
}