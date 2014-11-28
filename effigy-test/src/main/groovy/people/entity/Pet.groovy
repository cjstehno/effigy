package people.entity

import com.stehno.effigy.annotation.EffigyEntity
import com.stehno.effigy.annotation.Id
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by cjstehno on 11/28/2014.
 */
@EffigyEntity @ToString(includeNames = true) @EqualsAndHashCode
class Pet {

    @Id Long id

    String name
    String animal // fIXME: needs enum support
}

enum Animal {

    CAT,
    DOG,
    SNAKE,
    BIRD,
    RODENT
}
