package people.repository

import people.entity.Animal
import people.entity.Pet

/**
 * Simple repository for managing pets in the database.
 */
interface PetRepository {

    Long create(Pet pet)

    List<Pet> retrieve(long id)

    boolean delete(long id)

    List<Pet> findByAnimal(Animal animal)
}