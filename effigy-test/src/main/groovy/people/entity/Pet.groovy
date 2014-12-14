package people.entity

import com.stehno.effigy.annotation.Entity
import com.stehno.effigy.annotation.Id
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by cjstehno on 11/28/2014.
 */
@Entity @ToString(includeNames = true) @EqualsAndHashCode
class Pet {

    @Id Long id

    String name
    Animal animal
}
