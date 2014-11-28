package com.stehno.effigy.transform
import static org.codehaus.groovy.ast.ClassHelper.Long_TYPE
import static org.codehaus.groovy.ast.ClassHelper.long_TYPE

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
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

        FieldNode versionProperty = entityClassNode.fields.find { FieldNode f->
            f.annotations.find { AnnotationNode a-> a.classNode.name == 'com.stehno.effigy.annotation.Version' }
        }

        if( versionProperty && !(versionProperty.type in [Long_TYPE, long_TYPE]) ){
            throw new Exception('Currently the Version annotation may only be used on long or java.lang.Long fields.')
        }
    }
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