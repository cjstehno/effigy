package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Address
import people.entity.Animal
import people.entity.Person
import people.entity.Pet

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

        def idB = personRepository.create(new Person(PERSON_B))

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 2

        Person retrieved = personRepository.retrieve(id)

        assert retrieved.pets.size() == 2
        assert retrieved.pets.find { p-> p.name == 'Chester' }.animal == Animal.CAT
        assert retrieved.pets.find { p-> p.name == 'Fester' }.animal == Animal.SNAKE

        def people = personRepository.retrieveAll()
        assert people.size() == 2

        // delete non-pet person
        assert personRepository.delete(idB)

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 2

        assert personRepository.delete(id)

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 0
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 0
    }

    @Test void createWithPetAndUpdate() {
        Pet petA = petRepository.retrieve(petRepository.create(new Pet(name: 'Chester', animal: Animal.CAT)))
        Pet petB = petRepository.retrieve(petRepository.create(new Pet(name: 'Fester', animal: Animal.SNAKE)))

        Person personA = new Person(PERSON_A)
        personA.pets << petA
        personA.pets << petB

        def id = personRepository.create(personA)

        Person retrieved = personRepository.retrieve(id)

        assert retrieved.pets.size() == 2
        assert retrieved.pets.find { p -> p.name == 'Chester' }.animal == Animal.CAT
        assert retrieved.pets.find { p -> p.name == 'Fester' }.animal == Animal.SNAKE

        retrieved.lastName = 'Jones'
        retrieved.married = true
        retrieved.pets.remove(petB)

        personRepository.update(retrieved)

        def updated = personRepository.retrieve(id)
        assert updated.married
        assert updated.lastName == 'Jones'

        assert updated.pets.size() == 1
        assert updated.pets.find { p -> p.name == 'Chester' }.animal == Animal.CAT
    }

    @Test void deleteAll(){
        Pet petA = petRepository.retrieve(petRepository.create(new Pet(name: 'Chester', animal: Animal.CAT)))
        Pet petB = petRepository.retrieve(petRepository.create(new Pet(name: 'Fester', animal: Animal.SNAKE)))

        Person personA = new Person(PERSON_A)
        personA.pets << petA
        personA.pets << petB

        personRepository.create(personA)
        personRepository.create(new Person(PERSON_B))

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 2

        personRepository.deleteAll()

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 0
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 2
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'peoples_pets') == 0
    }

    @Test void createWithAddress() {
        Person personA = new Person(PERSON_A)
        personA.home = new Address('2121 Redbird Lane', 'Apartment 2', 'Glenview', 'OH', '84134')

        def id = personRepository.create(personA)
        assert id

        def retrieved = personRepository.retrieve(id)

        assert retrieved.firstName == 'John'
        assert retrieved.middleName == 'Q'
        assert retrieved.lastName == 'Public'
        assert !retrieved.married
        assert retrieved.home.line1 == '2121 Redbird Lane'
        assert retrieved.home.line2 == 'Apartment 2'
        assert retrieved.home.city == 'Glenview'
        assert retrieved.home.state == 'OH'
        assert retrieved.home.zip == '84134'

        retrieved.married = true
        retrieved.home = new Address('2121 Redbird Lane', 'Apartment 4', 'Cleavland', 'OH', '84134')

        personRepository.update(retrieved)

        retrieved = personRepository.retrieve(id)

        assert retrieved.firstName == 'John'
        assert retrieved.middleName == 'Q'
        assert retrieved.lastName == 'Public'
        assert retrieved.married
        assert retrieved.home.line1 == '2121 Redbird Lane'
        assert retrieved.home.line2 == 'Apartment 4'
        assert retrieved.home.city == 'Cleavland'
        assert retrieved.home.state == 'OH'
        assert retrieved.home.zip == '84134'
    }
}
