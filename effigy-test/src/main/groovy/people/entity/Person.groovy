package people.entity

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.annotation.OneToMany
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

    // @Ignored boolean active - TODO: add support for transient/ignored properties

    // TODO: support for component object
    // Occupation occupation (title, salary)

    /* FIXME: support
        onetoone - entity
        manytoone - entity (is this even valid for how my mapper works?)
        manytomany - collection, set, list, map
     */

    // -- supports collection, set, list, map
    @OneToMany(table = 'peoples_pets', entityId = 'person_id', associationId = 'pet_id')
    Set<Pet> pets = [] as Set<Pet>

    boolean isOver21(){
        new Date().year - birthDate.year >= 21
    }
}
