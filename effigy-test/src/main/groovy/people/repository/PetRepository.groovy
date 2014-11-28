package people.repository

import com.stehno.effigy.repository.CrudOperations
import people.entity.Pet

/**
 * Created by cjstehno on 11/28/2014.
 */
interface PetRepository extends CrudOperations<Pet,Long> {

}