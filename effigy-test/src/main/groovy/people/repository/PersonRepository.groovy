package people.repository

import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
interface PersonRepository {

    Long create(Person person)

    int count()

    Person retrieve(long id)

    List<Person> retrieveAll()

    boolean delete(long id)

    boolean deleteAll()

    boolean update(Person person)

    List<Person> findByMarried(boolean married)

    List<Person> findByLastName(String lastName)

    List<Person> findByFirstName(String firstName)
}