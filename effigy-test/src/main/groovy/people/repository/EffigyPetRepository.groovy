package people.repository

import com.stehno.effigy.annotation.Create
import com.stehno.effigy.annotation.Delete
import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.annotation.Retrieve
import people.entity.Animal
import people.entity.Pet

/**
 * Effigy-based implementation of the PetRepository interface.
 */
@Repository(forEntity = Pet)
abstract class EffigyPetRepository implements PetRepository {

    @Create
    abstract Long create(Pet pet)

    @Retrieve
    abstract List<Pet> retrieve(long id)

    @Delete
    abstract boolean delete(long id)

    @Retrieve(limit = 3)
    abstract List<Pet> findByAnimal(Animal animal)
}
