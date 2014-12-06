package people.entity

import com.stehno.effigy.annotation.*
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

    // -- supports collection, set, list, map
    @OneToMany(table = 'peoples_pets', entityId = 'person_id', associationId = 'pet_id')
    Set<Pet> pets = [] as Set<Pet>

    boolean isOver21(){
        new Date().year - birthDate.year >= 21
    }
}
