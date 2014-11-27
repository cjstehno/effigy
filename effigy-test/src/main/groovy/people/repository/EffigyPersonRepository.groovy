package people.repository

import com.stehno.effigy.annotation.EffigyRepository
import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
@EffigyRepository(forEntity = Person)
class EffigyPersonRepository implements PersonRepository {
}
