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

    static void injectCreateMethod( final ClassNode repositoryClassNode, final EntityModel entityInfo ){
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
                                constant("insert into ${entityInfo.table} (${entityInfo.findProperties(false).collect {it.columnName}.join(',')}) values (${entityInfo.findProperties(false).collect {'?'}.join(',')})" as String)
                                array(int.class) {
                                    entityInfo.findSqlTypes(false).each { typ ->
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
                            entityInfo.findProperties(false).each { pi ->
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
                        constant("set${entityInfo.identifier.propertyName.capitalize()}" as String)
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
            entityInfo.identifier.type,
            [new Parameter(entityInfo.type, 'entity')] as Parameter[],
            null,
            nodes[0] as Statement
        ))
    }
}
