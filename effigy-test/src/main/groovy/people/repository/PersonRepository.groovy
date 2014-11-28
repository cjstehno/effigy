package people.repository

import com.stehno.effigy.repository.CrudOperations
import people.entity.Person

/**
 * Created by cjstehno on 11/26/2014.
 */
interface PersonRepository extends CrudOperations<Person,Long>{

    /*
        FIXME: implement generated finders for simple things
        -- probably need an annotation to ignore methods on the impl side in case
           user wants to implement it themselves
     */
//    List<Person> findByMarried(boolean status)
}