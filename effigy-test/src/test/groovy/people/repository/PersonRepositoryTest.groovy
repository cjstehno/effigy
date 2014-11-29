package people.repository

import com.stehno.effigy.jdbc.EffigyEntityRowMapper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Animal
import people.entity.Person
import people.entity.Pet

class PersonRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private static final Map PERSON_A = [
        firstName: 'John',
        middleName: 'Q',
        lastName: 'Public',
        birthDate: Date.parse('MM/dd/yyyy', '05/28/1970'),
        married: false
    ]
    private static final Map PERSON_B = [
        firstName: 'Abe',
        middleName: 'A',
        lastName: 'Ableman',
        birthDate: Date.parse('MM/dd/yyyy', '05/28/1970'),
        married: true
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

        people.each { p->
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

        Person personB = new Person(PERSON_B + [id:id])

        personRepository.update(personB)

        assert personB == personRepository.retrieve(id)
    }

    @Test void createWithPet() {
        def petId = petRepository.create(new Pet(name: 'Chester', animal: Animal.CAT))
        Pet pet = petRepository.retrieve(petId)

        Person personA = new Person(PERSON_A)
        personA.pets << pet

        def id = personRepository.create(personA)
        assert id

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 1
    }
}
