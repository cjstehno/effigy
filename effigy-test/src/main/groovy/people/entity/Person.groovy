package people.entity

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.annotation.Version
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by cjstehno on 11/26/2014.
 */
@EffigyEntity(table='people') @EqualsAndHashCode @ToString(includeNames = true)
class Person {

    @Id Long id
    @Version Long version

    String firstName
    String middleName
    String lastName
    @Column('date_of_birth') Date birthDate
    boolean married

    boolean isOver21(){
        new Date().year - birthDate.year >= 21
    }
}
