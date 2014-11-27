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
 * Created by cjstehno on 11/27/2014.
 */
class RetrieveMethodInjector {

    static void injectRetrieveMethod( final ClassNode repositoryClassNode, final EntityInfo entityInfo ){
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
            'create',
            Modifier.PUBLIC,
            entityInfo.idType,
            [new Parameter(entityInfo.type, 'entity')] as Parameter[],
            null,
            nodes[0] as Statement
        ))
    }
}
