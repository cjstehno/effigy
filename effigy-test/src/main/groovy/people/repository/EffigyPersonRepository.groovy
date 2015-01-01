package people.repository

import com.stehno.effigy.annotation.*
import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
@Repository(forEntity = Person)
abstract class EffigyPersonRepository implements PersonRepository {

    @Create
    abstract Long create(Person person)

    @Count
    abstract int count()

    @Retrieve
    abstract Person retrieve(long id)

    @Retrieve
    abstract List<Person> retrieveAll()

    @Delete
    abstract boolean delete(long id)

    @Delete
    abstract boolean deleteAll()

    @Update
    abstract boolean update(Person person)

    @Retrieve
    abstract List<Person> findByLastName(String lastName)

    @Retrieve
    abstract List<Person> findByMarried(boolean married)

    @Retrieve(limit = 2)
    abstract List<Person> findByFirstName(String firstName)
}
