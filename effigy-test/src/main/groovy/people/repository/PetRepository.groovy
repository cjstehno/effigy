package people.repository

import com.stehno.effigy.annotation.Limited
import com.stehno.effigy.repository.CrudOperations
import people.entity.Animal
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
interface PetRepository extends CrudOperations<Pet, Long> {

    @Limited(3)
    List<Pet> findByAnimal(Animal animal)
}