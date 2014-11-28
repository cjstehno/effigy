package com.stehno.effigy.transform

import groovy.text.GStringTemplateEngine
import groovy.text.TemplateEngine
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase

import java.lang.reflect.Modifier

/**
 * Created by cjstehno on 11/28/2014.
 */
class UpdateMethodInjector {

    private static final TemplateEngine templateEngine = new GStringTemplateEngine()

    static void injectUpdateMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        try {
            def columnUpdates = []
            def vars = []

            model.findProperties(false).each { p ->
                columnUpdates << "${p.columnName}=?"
                vars << "entity.${p.propertyName}"
            }

            String entityVersionUpdate = ''
            String versionCriteria = ''
            String versionParam = ''
            if( model.versioner){
                entityVersionUpdate = """
                    def currentVersion = entity.${model.versioner.propertyName} ?: 0
                    entity.${model.versioner.propertyName} = currentVersion + 1
                """
                versionCriteria = "and ${model.versioner.columnName}=?"
                versionParam = ",currentVersion"
            }

            def nodes = new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, true, """
                $entityVersionUpdate
                jdbcTemplate.update(
                    'update people set ${columnUpdates.join(',')} where ${model.identifier.columnName}=? $versionCriteria',
                    ${vars.join(',')},
                    entity.${model.identifier.propertyName}$versionParam
                )
            """)

            println "There are ${nodes.size()} nodes"

            repositoryClassNode.addMethod(new MethodNode(
                'update',
                Modifier.PUBLIC,
                ClassHelper.VOID_TYPE,
                [new Parameter(model.type, 'entity')] as Parameter[],
                null,
                nodes[0] as Statement
            ))
        } catch (ex) {
            ex.printStackTrace()
        }
    }
}
