package people.repository

import groovy.sql.Sql
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Animal
import people.entity.Person
import people.entity.Pet

import java.sql.ResultSet
import java.sql.SQLException

class PersonRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private static final Map PERSON_A = [
        firstName : 'John',
        middleName: 'Q',
        lastName  : 'Public',
        birthDate : Date.parse('MM/dd/yyyy', '05/28/1970'),
        married   : false
    ]
    private static final Map PERSON_B = [
        firstName : 'Abe',
        middleName: 'A',
        lastName  : 'Ableman',
        birthDate : Date.parse('MM/dd/yyyy', '05/28/1970'),
        married   : true
    ]

    private PersonRepository personRepository
    private PetRepository petRepository

    @Before void before() {
        personRepository = new EffigyPersonRepository(
            jdbcTemplate: database.jdbcTemplate
        )
        petRepository = new EffigyPetRepository(
            jdbcTemplate: database.jdbcTemplate
        )
    }

    @Test void create() {
        Person personA = new Person(PERSON_A)

        def id = personRepository.create(personA)

        assert id == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1

        def result = personRepository.retrieve(1)
        assert result == personA

        Person personB = new Person(PERSON_B)

        def idB = personRepository.create(personB)

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 2

        def people = personRepository.retrieveAll()
        assert people.size() == 2

        people.each { p ->
            println p
        }

        assert !personRepository.delete(100)

        assert personRepository.delete(1)
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1

        assert personRepository.deleteAll()
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 0
    }

    @Test void update() {
        Person personA = new Person(PERSON_A)

        def id = personRepository.create(personA)

        assert personA == personRepository.retrieve(id)

        Person personB = new Person(PERSON_B + [id: id])

        personRepository.update(personB)

        assert personB == personRepository.retrieve(id)
    }

    @Test void createWithPet() {
        Pet petA = petRepository.retrieve(petRepository.create(new Pet(name: 'Chester', animal: Animal.CAT)))
        Pet petB = petRepository.retrieve(petRepository.create(new Pet(name: 'Fester', animal: Animal.SNAKE)))

        Person personA = new Person(PERSON_A)
        personA.pets << petA
        personA.pets << petB

        def id = personRepository.create(personA)
        assert id

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 2

//        Person retrieved = personRepository.retrieve(id)
        Person retrieved = retrieveWithRelations(id)

        assert retrieved.pets.size() == 2
        assert retrieved.pets.find { p-> p.name == 'Chester' }.animal == Animal.CAT
        assert retrieved.pets.find { p-> p.name == 'Fester' }.animal == Animal.SNAKE
    }

    Person retrieveWithRelations(long id) {
        database.jdbcTemplate.query(
            '''
            select people.id as people_id,
              people.version as people_version,
              people.first_name as people_first_name,
              people.middle_name as people_middle_name,
              people.last_name as people_last_name,
              people.date_of_birth as people_date_of_birth,
              people.married as people_married,

              pets.id as pets_id,
              pets.name as pets_name,
              pets.animal as pets_animal

              from people
                LEFT OUTER JOIN peoples_pets on people.id=peoples_pets.person_id
                left OUTER join pets on pets.id=peoples_pets.pet_id
              where people.id=?
            ''',
            new AssociationRowMapper<Person>(),
            id
        )
    }
}

// FIXME: need prefixes on row mappers
class AssociationRowMapper<T> implements ResultSetExtractor<T> {

    @Override
    T extractData(final ResultSet rs) throws SQLException, DataAccessException {
        Person person = null
        while( rs.next() ){
            if( !person ){
                person = Person.ROW_MAPPER.mapRow(rs, 0)
            }

            person.pets << Pet.ROW_MAPPER.mapRow(rs,0)
        }
        person
    }
}
