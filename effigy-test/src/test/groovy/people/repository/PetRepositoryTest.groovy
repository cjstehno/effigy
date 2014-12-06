package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Animal
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
class PetRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private static final Map PET_A = [
        name  : 'Wolfie',
        animal: Animal.DOG
    ]

    private PetRepository petRepository

    @Before void before() {
        petRepository = new EffigyPetRepository(
            jdbcTemplate: database.jdbcTemplate
        )
    }

    @Test void operations() {
        def petId = petRepository.create(new Pet(PET_A))

        def petA = petRepository.retrieve(petId)
        assert petA.name == 'Wolfie'
        assert petA.animal == Animal.DOG

        assert petRepository.delete(petId)

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 0
    }
}
