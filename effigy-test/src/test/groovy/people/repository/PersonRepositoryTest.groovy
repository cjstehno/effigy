package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Person

class PersonRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private PersonRepository personRepository

    @Before void before() {
        personRepository = new EffigyPersonRepository(
            jdbcTemplate: database.jdbcTemplate
        )
    }

    @Test void create() {
        Person person = new Person(
            firstName: 'John',
            middleName: 'Q',
            lastName: 'Public',
            birthDate: Date.parse('MM/dd/yyyy', '05/28/1970'),
            married: false
        )

        def id = personRepository.save(person)

        assert id == 1
        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'people') == 1
    }
}
