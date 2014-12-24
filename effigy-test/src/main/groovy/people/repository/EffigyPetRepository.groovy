package people.repository

import com.stehno.effigy.annotation.*
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
@Repository(forEntity = Pet)
@CreateOperations
@RetrieveOperations
@UpdateOperations
@DeleteOperations
@FindOperations
abstract class EffigyPetRepository implements PetRepository {

}
