package com.stehno.effigy.transform

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.Statement
import org.springframework.jdbc.core.PreparedStatementCreatorFactory
import org.springframework.jdbc.support.GeneratedKeyHolder

import java.lang.reflect.Modifier

/**
 * Injects the code for the repository entity create method.
 */
class CreateMethodInjector {

    static void injectCreateMethod(final ClassNode repositoryClassNode, final EntityModel model) {
        def nodes = new AstBuilder().buildFromSpec {
            block {
                if( model.versioner ){
                    expression {
                        methodCall {
                            variable('entity')
                            constant("set${model.versioner.propertyName.capitalize()}" as String)
                            argumentList {
                                constant(0)
                            }
                        }
                    }
                }

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
                                constant("insert into ${model.table} (${model.findProperties(false).collect { it.columnName }.join(',')}) values (${model.findProperties(false).collect { '?' }.join(',')})" as String)
                                array(int.class) {
                                    model.findSqlTypes(false).each { typ ->
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
                            model.findProperties(false).each { pi ->
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

                expression {
                    methodCall {
                        variable('entity')
                        constant("set${model.identifier.propertyName.capitalize()}" as String)
                        argumentList {
                            property {
                                variable('keys')
                                constant('key')
                            }
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
            'create',
            Modifier.PUBLIC,
            model.identifier.type,
            [new Parameter(model.type, 'entity')] as Parameter[],
            null,
            nodes[0] as Statement
        ))
    }
}
