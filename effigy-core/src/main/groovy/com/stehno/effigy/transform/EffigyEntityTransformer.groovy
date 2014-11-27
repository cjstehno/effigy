package com.stehno.effigy.transform
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
/**
 * Created by cjstehno on 11/26/2014.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyEntityTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode



//        injectRowMapper(entityClassNode)
    }

//    private static void injectRowMapper(ClassNode entityClassNode){
//        def nodes = new AstBuilder().buildFromSpec {
//            classNode('PersonRowMapper', Modifier.PUBLIC){
//                interfaces {
//                    classNode(RowMapper)
//                }
//                method('mapRow', Modifier.PUBLIC, entityClassNode.typeClass){
//                    parameter(rs:ResultSet, rowNum:int.class)
//                }
//            }
//        }
//    }
}

/*
class EffigyPropertyMapper implements RowMapper<Person> {

    @Override
    Person mapRow(ResultSet rs, int rowNum) throws SQLException {
        new Person(
            id: rs.getObject('id'),
            firstName: rs.getObject('first_name'),
            middleName: rs.getObject('middle_name'),
            lastName: rs.getObject('last_name'),
            birthDate: rs.getObject('date_of_birth'),
            married: rs.getObject('married')
        )
    }
}
 */