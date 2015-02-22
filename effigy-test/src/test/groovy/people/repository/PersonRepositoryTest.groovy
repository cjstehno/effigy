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

import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable
import static people.entity.Animal.*

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
    private PetRepository petRepository
    private JobRepository jobRepository

    @Before void before() {
        personRepository = new EffigyPersonRepository(jdbcTemplate: database.jdbcTemplate)
        petRepository = new EffigyPetRepository(jdbcTemplate: database.jdbcTemplate)
        jobRepository = new EffigyJobRepository(jdbcTemplate: database.jdbcTemplate)
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
        assert !people[0].job
        assert !people[0].pets

        assert people[1].id == idB
        assert people[1].version == 2
        assertProperties(PERSON_B, people[1])
        assert people[1].job.title == 'Big Kahuna'
        assert people[1].pets.size() == 2

        assert people[2].id == idC
        assert people[2].version == 2
        assertProperties(PERSON_C, people[2])
        assert !people[2].job
        assert people[2].pets.size() == 1
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

        assertRowCount 'peoples_pets', 1
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

        assertRowCount 'peoples_pets', 0
    }

    @Test void findTwo() {
        def (idA, idB, idC) = createThree()

        def two = personRepository.findTwo()
        assert two.size() == 2

        assert two[0].id == idC
        assertProperties(PERSON_C, two[0])

        assert two[1].id == idB
        assertProperties(PERSON_B, two[1])
    }

    @Test void findPaged() {
        def (idA, idB, idC) = createThree()

        def page = personRepository.findPaged(0, 1)
        assert page.size() == 1
        assertProperties(PERSON_A, page[0])

        page = personRepository.findPaged(1, 1)
        assert page.size() == 1
        assertProperties(PERSON_C, page[0])

        page = personRepository.findPaged(2, 1)
        assert page.size() == 1
        assertProperties(PERSON_B, page[0])
    }

    @Test void findByLastName() {
        def (idA, idB, idC) = createThree()

        def people = personRepository.findByLastName('Smith')
        assert people.size() == 2

        people = personRepository.findByLastName('Public')
        assert people.size() == 1
    }

    @Test void findLastNames() {
        def (idA, idB, idC) = createThree()

        def lastNames = personRepository.findLastNames(true)

        assert lastNames.size() == 2
        assert lastNames.containsAll('Public', 'Smith')
    }

    private static void assertProperties(Map props, Person entity) {
        props.each { k, v ->
            assert entity[k] == v
        }
    }

    private List createThree() {
        def petA = petRepository.retrieve(petRepository.create(name: 'Felix', animal: CAT))
        def petB = petRepository.retrieve(petRepository.create(name: 'Spot', animal: DOG))
        def petC = petRepository.retrieve(petRepository.create(name: 'Nicodemus', animal: RODENT))

        def jobA = jobRepository.retrieve(jobRepository.create(title: 'Big Kahuna'))
        def jobB = jobRepository.retrieve(jobRepository.create(title: 'Nobody'))

        assert petRepository.countAll() == 3

        assert jobRepository.count(jobA.id) == 1
        assert jobRepository.count(jobB.id) == 1

        def idA = personRepository.create(new Person(PERSON_A))

        def idB = personRepository.create(new Person(PERSON_B))
        Person personB = personRepository.retrieve(idB)
        personB.pets.addAll([petA, petB])
        personB.job = jobA
        assert personRepository.update(personB)

        assertRowCount 'peoples_pets', 2

        def idC = personRepository.create(new Person(PERSON_C))
        Person personC = personRepository.retrieve(idC)
        personC.pets.add(petC)
        assert personRepository.update(personC)

        assertRowCount 'peoples_pets', 3

        [idA, idB, idC]
    }

    private void assertRowCount(String table, int count) {
        assert countRowsInTable(database.jdbcTemplate, table) == count
    }
}
