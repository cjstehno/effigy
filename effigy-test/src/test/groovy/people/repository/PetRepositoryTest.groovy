package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Animal
import people.entity.Pet

import static people.entity.Animal.CAT
import static people.entity.Animal.DOG

/**
 * Created by cjstehno on 11/28/2014.
 */
class PetRepositoryTest {

    @Rule
    public DatabaseEnvironment database = new DatabaseEnvironment()

    private static final Map PET_A = [
        name  : 'Wolfie',
        animal: DOG
    ]

    private PetRepository petRepository

    @Before
    void before() {
        petRepository = new EffigyPetRepository(
            jdbcTemplate: database.jdbcTemplate
        )
    }

    @Test
    void operations() {
        def petId = petRepository.create(new Pet(PET_A))

        def petA = petRepository.retrieve(petId)
        assert petA.name == 'Wolfie'
        assert petA.animal == DOG

        assert petRepository.delete(petId)

        assert JdbcTestUtils.countRowsInTable(database.jdbcTemplate, 'pets') == 0
    }

    @Test
    void limitedFinder() {
        def idA = petRepository.create(new Pet(name: 'Sparky', animal: DOG))
        def idB = petRepository.create(new Pet(name: 'Rufus', animal: DOG))
        def idC = petRepository.create(new Pet(name: 'Fluffy', animal: DOG))
        def idD = petRepository.create(new Pet(name: 'Wolfie', animal: DOG))
        def idE = petRepository.create(new Pet(name: 'Felix', animal: CAT))

        def found = petRepository.findByAnimal(DOG)
        assert found.size() == 3
        assert found*.id.containsAll([idA, idB, idC])

        found = petRepository.findByAnimal(CAT)
        assert found.size() == 1
        assert found[0].id == idE
    }
}
