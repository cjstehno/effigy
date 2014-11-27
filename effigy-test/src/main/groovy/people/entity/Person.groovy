package people.entity

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.Effigy
import com.stehno.effigy.annotation.Id

/**
 * Created by cjstehno on 11/26/2014.
 */
@Effigy(table='people')
class Person {

    @Id Long id
    String firstName
    String middleName
    String lastName
    @Column('date_of_birth') Date birthDate
    boolean married

    boolean isOver21(){
        new Date().year - birthDate.year >= 21
    }
}
