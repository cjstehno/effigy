/*
 * Copyright (c) 2014 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package people.entity

import com.stehno.effigy.annotation.*
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Entity representing a person in the database.
 */
@Entity(table = 'people') @EqualsAndHashCode @ToString(includeNames = true)
class Person {

    @Id Long id
    @Version long version

    String firstName
    String middleName
    String lastName
    @Column('date_of_birth') Date birthDate
    boolean married

    @Association(joinTable = 'peoples_pets', entityColumn = 'person_id', assocColumn = 'pet_id')
    Set<Pet> pets = [] as Set<Pet>

    @Embedded Address home

    @Component(lookupTable = 'employers') Address work

    Job job

    // this will not be resolved as a field
    boolean isOver21() {
        new Date().year - birthDate.year >= 21
    }
}