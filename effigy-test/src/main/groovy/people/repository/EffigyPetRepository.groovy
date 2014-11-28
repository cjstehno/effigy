package people.repository

import com.stehno.effigy.annotation.EffigyRepository
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
@EffigyRepository(forEntity = Pet)
abstract class EffigyPetRepository implements PetRepository {
}
