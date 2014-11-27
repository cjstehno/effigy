package com.stehno.effigy.transform

import static com.stehno.effigy.transform.AnnotationUtils.extractClass
import static com.stehno.effigy.transform.AnnotationUtils.extractString

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.Effigy
import com.stehno.effigy.annotation.Id
import groovy.text.Template
import groovy.transform.ToString
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import java.lang.reflect.Modifier
import java.sql.Types

/**
 * Created by cjstehno on 11/26/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyRepositoryTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        println "EffigyRepository: ${nodes[0]}"

        source.getAST().addImport(null, ClassHelper.make(GeneratedKeyHolder))

        injectJdbcTemplate(nodes[1])

        ClassNode entityClassNode = extractClass(nodes[0], 'forEntity')
        AnnotationNode effigyAnnotNode = entityClassNode.getAnnotations(new ClassNode(Effigy))[0]

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

        println entityInfo

        injectSaveMethod(nodes[1] as ClassNode, entityInfo)
    }

    private static void injectSaveMethod(final ClassNode repositoryClassNode, final EntityInfo entityInfo) {
        def nodes = new AstBuilder().buildFromSpec {
            block {
                expression {
                    declaration {
                        variable('keys')
                        token('=')
                        constructorCall(GeneratedKeyHolder) {
                            argumentList {}
                        }
                    }
                }

                expression {
                    declaration {
                        variable('factory')
                        token('=')
                        constructorCall(PreparedStatementCreatorFactory) {
                            argumentList {
                                constant("insert into ${entityInfo.table} (${entityInfo.fieldNamesString(false)}) values (${entityInfo.placeholderString(false)})" as String)
                                array(int.class) {
                                    entityInfo.types(false).each { typ ->
                                        constant(typ)
                                    }
                                }
                            }
                        }
                    }
                }

                expression {
                    declaration {
                        variable('paramValues')
                        token('=')
                        array(Object) {
                            entityInfo.propertyInfo(false).each { pi ->
                                property {
                                    variable('entity')
                                    constant(pi.propertyName)
                                }
                            }
                        }
                    }
                }

                expression {
                    declaration {
                        variable('psc')
                        token('=')
                        methodCall {
                            variable 'factory'
                            constant 'newPreparedStatementCreator'
                            argumentList {
                                variable('paramValues')
                            }
                        }
                    }
                }

                expression {
                    methodCall {
                        variable('jdbcTemplate')
                        constant('update')
                        argumentList {
                            variable('psc')
                            variable('keys')
                        }
                    }
                }

                returnStatement {
                    property {
                        variable('keys')
                        constant('key')
                    }
                }
            }
        }

        repositoryClassNode.addMethod(new MethodNode(
            'save',
            Modifier.PUBLIC,
            entityInfo.idType,
            [new Parameter(entityInfo.type, 'entity')] as Parameter[],
            null,
            nodes[0] as Statement
        ))
    }

    private void injectJdbcTemplate(ClassNode repositoryClassNode) {
        FieldNode jdbcTemplateFieldNode = new FieldNode(
            'jdbcTemplate', Modifier.PRIVATE, new ClassNode(JdbcTemplate), repositoryClassNode, new EmptyExpression()
        )

        jdbcTemplateFieldNode.addAnnotation(new AnnotationNode(new ClassNode(Autowired)))

        repositoryClassNode.addField(jdbcTemplateFieldNode)
    }

    private static List<ASTNode> build(Template template, Map attrs) {
        def text = template.make(attrs).toString()
        println text

        try {
            new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, true, text)
        } catch (ex) {
            ex.printStackTrace()
            return null
        }
    }
}

@ToString(includeNames = true)
class EntityInfo {
    ClassNode type
    String table
    List<EntityPropertyInfo> props = []

    ClassNode getIdType() {
        props.find { it.id }.type
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
}

@ToString(includeNames = true)
class EntityPropertyInfo {
    boolean id
    String fieldName
    String propertyName
    ClassNode type
}