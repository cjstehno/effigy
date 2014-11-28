package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
        println petA
    }
}
