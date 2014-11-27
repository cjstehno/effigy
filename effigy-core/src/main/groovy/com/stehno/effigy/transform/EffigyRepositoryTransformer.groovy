package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.CreateMethodInjector.injectCreateMethod
import static EntityModel.extractEntityInfo
import static com.stehno.effigy.transform.RetrieveMethodInjector.injectRetrieveMethod
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/26/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyRepositoryTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode effigyAnnotNode = nodes[0] as AnnotationNode
        ClassNode repositoryClassNode = nodes[1] as ClassNode

        /// FIXME: use the CrudOperations interface as a marker to turn on/off crud injection

        injectJdbcTemplate repositoryClassNode
        removeAbstract repositoryClassNode

        ClassNode entityClassNode = extractClass(effigyAnnotNode, 'forEntity')
        EntityModel entityInfo = extractEntityInfo(entityClassNode)
        println entityInfo

        injectRowMapper(entityClassNode, entityInfo)

        injectCreateMethod repositoryClassNode, entityInfo
        injectRetrieveMethod repositoryClassNode, entityInfo
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

        entityClassNode.addField(
            new FieldNode(
                'ROW_MAPPER',
                Modifier.STATIC | Modifier.PUBLIC,
                makeClassSafe(RowMapper),
                entityClassNode,
                expression
            )
        )
    }

    private static void removeAbstract(ClassNode repositoryClassNode) {
        if (Modifier.isAbstract(repositoryClassNode.modifiers)) {
            repositoryClassNode.modifiers = Modifier.PUBLIC
        }
    }

    private static void injectJdbcTemplate(ClassNode repositoryClassNode) {
        FieldNode jdbcTemplateFieldNode = new FieldNode(
            'jdbcTemplate', Modifier.PRIVATE, new ClassNode(JdbcTemplate), repositoryClassNode, new EmptyExpression()
        )

        jdbcTemplateFieldNode.addAnnotation(new AnnotationNode(new ClassNode(Autowired)))

        repositoryClassNode.addField(jdbcTemplateFieldNode)
    }
}
