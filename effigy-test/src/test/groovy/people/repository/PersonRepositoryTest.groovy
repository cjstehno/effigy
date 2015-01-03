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

package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import people.DatabaseEnvironment
import people.entity.Address
import people.entity.Person

/**
 * Created by cjstehno on 1/1/15.
 */
class PersonRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private static final Map PERSON_A = [
        firstName: 'John', middleName: 'Q', lastName: 'Public', birthDate: new Date(), married: false,
        home     : new Address('123 E West St', 'Apt 15', 'Plainview', 'OH', '12345'),
        work     : new Address('4242 Empire Dr', 'Ste 1234', 'Masterplan', 'OH', '45643')
    ]

    private static final Map PERSON_B = [
        firstName: 'John', middleName: 'M', lastName: 'Smith', birthDate: new Date(), married: true,
        work     : new Address('8296 E First St', 'Ste 34', 'Blahville', 'NE', '85643')
    ]

    private static final Map PERSON_C = [
        firstName: 'Jane', middleName: 'Y', lastName: 'Smith', birthDate: new Date(), married: true
    ]

    private PersonRepository personRepository

    @Before void before() {
        personRepository = new EffigyPersonRepository(
            jdbcTemplate: database.jdbcTemplate
        )
    }

    @Test void create() {
        def (idA, idB, idC) = createThree()
        assert idA == 1
        assert idB == 2
        assert idC == 3
    }

    @Test void retrieveAll() {
        def (idA, idB, idC) = createThree()

        def people = personRepository.retrieveAll()
        assert people.size() == 3

        assert people[0].id == idA
        assert people[0].version == 1
        assertProperties(PERSON_A, people[0])

        assert people[1].id == idB
        assert people[1].version == 1
        assertProperties(PERSON_B, people[1])

        assert people[2].id == idC
        assert people[2].version == 1
        assertProperties(PERSON_C, people[2])
    }

    @Test void retrieve() {
        def (idA, idB, idC) = createThree()

        def person = personRepository.retrieve(idA)
        assertProperties(PERSON_A, person)

        person = personRepository.retrieve(idB)
        assertProperties(PERSON_B, person)

        person = personRepository.retrieve(idC)
        assertProperties(PERSON_C, person)
    }

    @Test void update() {
        def id = personRepository.create(new Person(PERSON_C))

        Person person = personRepository.retrieve(id)
        println person

        person.lastName = 'Jones'
        person.married = false

        assert personRepository.update(person)

        person = personRepository.retrieve(id)
        assertProperties(person, firstName: 'Jane', middleName: 'Y', lastName: 'Jones', married: false, version: 2)
    }

    @Test void delete() {
        def (idA, idB, idC) = createThree()

        assert personRepository.delete(idB)

        assert personRepository.countAll() == 2
        assert personRepository.count(idA) == 1
        assert personRepository.count(idB) == 0
        assert personRepository.count(idC) == 1

        assert personRepository.exists(idA)
        assert !personRepository.exists(idB)
        assert personRepository.exists(idC)

        assert personRepository.retrieve(idA)
        assert !personRepository.retrieve(idB)
        assert personRepository.retrieve(idC)
    }

    @Test void deleteAll() {
        def (idA, idB, idC) = createThree()

        assert personRepository.countAll() == 3
        assert personRepository.count(idA) == 1
        assert personRepository.count(idB) == 1
        assert personRepository.count(idC) == 1

        assert personRepository.deleteAll()

        assert personRepository.countAll() == 0
        assert personRepository.count(idA) == 0
        assert personRepository.count(idB) == 0
        assert personRepository.count(idC) == 0

        assert !personRepository.exists(idA)
        assert !personRepository.exists(idB)
        assert !personRepository.exists(idC)

        assert !personRepository.retrieve(idA)
        assert !personRepository.retrieve(idB)
        assert !personRepository.retrieve(idC)
    }

    private static void assertProperties(Map props, Person entity) {
        props.each { k, v ->
            assert entity[k] == v
        }
    }

    private List createThree() {
        [
            personRepository.create(new Person(PERSON_A)),
            personRepository.create(new Person(PERSON_B)),
            personRepository.create(new Person(PERSON_C))
        ]
    }
}
