package people.repository

import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.annotation.Retrieve
import com.stehno.effigy.repository.CrudRepository
import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
@Repository(forEntity = Person)
abstract class EffigyPersonRepository extends CrudRepository<Person, Long> implements PersonRepository {

    @Retrieve
    abstract List<Person> findByLastName(String lastName)

    @Retrieve
    abstract List<Person> findByMarried(boolean married)

    @Retrieve(limit = 2)
    abstract List<Person> findByFirstName(String firstName)
}
