package people.repository

import com.stehno.effigy.annotation.CrudOperations
import com.stehno.effigy.annotation.Repository
import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
@Repository(forEntity = Person) @CrudOperations
abstract class EffigyPersonRepository implements PersonRepository {

    abstract List<Person> findByLastName(String lastName)

    List<Person> findBySomething(String something) {
        throw new UnsupportedOperationException('This is not valid')
    }
}
