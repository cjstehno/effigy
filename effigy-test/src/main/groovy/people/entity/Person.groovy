package people.entity

import com.stehno.effigy.annotation.*
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@Entity(table = 'people') @EqualsAndHashCode @ToString(includeNames = true)
class Person {

    @Id Long id
    @Version Long version

    String firstName
    String middleName
    String lastName
    @Column('date_of_birth') Date birthDate
    boolean married

    @OneToMany(table = 'peoples_pets', entityId = 'person_id', associationId = 'pet_id')
    Set<Pet> pets = [] as Set<Pet>

    @Embedded Address home

    @Component(lookupTable = 'employers') Address work

    // this will not be resolved as a field
    boolean isOver21() {
        new Date().year - birthDate.year >= 21
    }
}
