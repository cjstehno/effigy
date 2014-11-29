package com.stehno.effigy.transform
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.AstUtils.codeS
import static com.stehno.effigy.transform.EntityModel.registerEntityModel
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.jdbc.EffigyEntityRowMapper
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.reflect.Modifier
import java.sql.ResultSet
/**
 * Created by cjstehno on 11/26/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyEntityTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode

        FieldNode versionProperty = entityClassNode.fields.find { FieldNode f->
            f.annotations.find { AnnotationNode a-> a.classNode.name == 'com.stehno.effigy.annotation.Version' }
        }

        if( versionProperty && !(versionProperty.type in [Long_TYPE, long_TYPE]) ){
            throw new Exception('Currently the Version annotation may only be used on long or java.lang.Long fields.')
        }

        EntityModel model = registerEntityModel(entityClassNode)

        ClassNode mapperClassNode = buildRowMapper(model, source)

        injectRowMapperAccessor(entityClassNode, mapperClassNode, model)
    }

    private static void injectRowMapperAccessor(ClassNode entityClassNode, ClassNode mapperClassNode, EntityModel model) {
        entityClassNode.addMethod(new MethodNode(
            'rowMapper',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(mapperClassNode),
            [new Parameter(STRING_TYPE, 'prefix', constX(''))] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(mapperClassNode), args(new MapExpression([new MapEntryExpression(constX('prefix'), varX('prefix'))]))))
        ))

        info EffigyEntityTransformer, 'Injected row mapper helper method for {}', model.type
    }

    private static ClassNode buildRowMapper(EntityModel model, SourceUnit source) {
        ClassNode mapperClassNode = null
        try {
            mapperClassNode = new ClassNode(
                "${model.type.packageName}.${model.type.nameWithoutPackage}RowMapper",
                Modifier.PUBLIC,
                makeClassSafe(EffigyEntityRowMapper),
                [] as ClassNode[],
                [] as MixinNode[]
            )

            mapperClassNode.addMethod(new MethodNode(
                'newEntity',
                Modifier.PROTECTED,
                newClass(model.type),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(ctorX(newClass(model.type)))
            ))

            mapperClassNode.addMethod(new MethodNode(
                'mapping',
                Modifier.PROTECTED,
                VOID_TYPE,
                [new Parameter(make(ResultSet), 'rs'), new Parameter(OBJECT_TYPE, 'entity')] as Parameter[],
                [] as ClassNode[],
                block(
                    codeS(
                        '''
                        def ent = entity
                        <% model.findProperties().each { p-> %>
                            ent.${p.propertyName} = rs.getObject( prefix + '${p.columnName}' )
                        <% } %>
                        ''',
                        model: model
                    )
                )
            ))

            source.AST.addClass(mapperClassNode)

            info EffigyEntityTransformer, 'Injected row mapper ({}) for {}', mapperClassNode.name, model.type

        } catch (ex) {
            ex.printStackTrace()
        }
        mapperClassNode
    }
}
