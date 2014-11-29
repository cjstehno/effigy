package com.stehno.effigy.transform

import static com.stehno.effigy.transform.EntityModel.registerEntityModel
import static org.codehaus.groovy.ast.ClassHelper.Long_TYPE
import static org.codehaus.groovy.ast.ClassHelper.long_TYPE
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe

import com.stehno.effigy.jdbc.EffigyEntityRowMapper
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.jdbc.core.RowMapper

import java.lang.reflect.Modifier
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

        EntityModel entityInfo = registerEntityModel(entityClassNode)

        injectRowMapper(entityClassNode, entityInfo)
    }

    private static void injectRowMapper(ClassNode entityClassNode, EntityModel entityInfo) {
        def mapEntries = entityInfo.findProperties().collect { p ->
            new MapEntryExpression(
                new ConstantExpression(p.propertyName),
                new ConstantExpression(p.columnName)
            )
        }

        Expression expression = new ConstructorCallExpression(
            makeClassSafe(EffigyEntityRowMapper),
            new ArgumentListExpression([
                new ClassExpression(entityInfo.type),
                new MapExpression(mapEntries)
            ])
        )

        entityClassNode.addField(new FieldNode(
            'ROW_MAPPER',
            Modifier.STATIC | Modifier.PUBLIC,
            makeClassSafe(RowMapper),
            entityClassNode,
            expression
        ))
    }
}
