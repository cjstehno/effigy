package people.repository

import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.annotation.Retrieve
import com.stehno.effigy.repository.CrudRepository
import people.entity.Animal
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
@Repository(forEntity = Pet)
abstract class EffigyPetRepository extends CrudRepository<Pet, Long> implements PetRepository {

    @Retrieve(limit = 3)
    List<Pet> findByAnimal(Animal animal) {
        return null
    }
}
