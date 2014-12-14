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

import static org.mockito.Mockito.when

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.jdbc.core.ResultSetExtractor
import people.entity.*

import java.sql.ResultSet

@RunWith(MockitoJUnitRunner)
class PersonResultSetExtractorTest {

    @Mock private ResultSet resultSet

    @Test void 'extract'() {
        def now = new Date()

        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false)

        when(resultSet.getObject('people_id')).thenReturn(123L)
        when(resultSet.getObject('people_version')).thenReturn(2L)
        when(resultSet.getObject('people_first_name')).thenReturn('John')
        when(resultSet.getObject('people_middle_name')).thenReturn('Q')
        when(resultSet.getObject('people_last_name')).thenReturn('Public')
        when(resultSet.getObject('people_date_of_birth')).thenReturn(now)
        when(resultSet.getObject('people_married')).thenReturn(true)

        when(resultSet.getObject('people_home_line1')).thenReturn('123 W East St')
        when(resultSet.getObject('people_home_line2')).thenReturn('Apt 42')
        when(resultSet.getObject('people_home_city')).thenReturn('Somewhere')
        when(resultSet.getObject('people_home_state')).thenReturn('AZ')
        when(resultSet.getObject('people_home_zip')).thenReturn('85701')

        when(resultSet.getObject('work_line1')).thenReturn('42 Everything Ave')
        when(resultSet.getObject('work_line2')).thenReturn('Ste 84')
        when(resultSet.getObject('work_city')).thenReturn('Nowhere')
        when(resultSet.getObject('work_state')).thenReturn('AZ')
        when(resultSet.getObject('work_zip')).thenReturn('85702')

        when(resultSet.getObject('job_id')).thenReturn(99)
        when(resultSet.getObject('job_title')).thenReturn('Big Muckemuck')

        when(resultSet.getObject('pets_id')).thenReturn(22).thenReturn(23)
        when(resultSet.getObject('pets_name')).thenReturn('Fluffy').thenReturn('Rover')
        when(resultSet.getObject('pets_animal')).thenReturn('CAT').thenReturn('DOG')

        ResultSetExtractor<Person> extractor = Person.associationExtractor()
        Person person = extractor.extractData(resultSet)

        assert person.id == 123L
        assert person.version == 2L
        assert person.firstName == 'John'
        assert person.middleName == 'Q'
        assert person.lastName == 'Public'
        assert person.birthDate == now
        assert person.married
        assert person.home == new Address('123 W East St', 'Apt 42', 'Somewhere', 'AZ', '85701')
        assert person.work == new Address('42 Everything Ave', 'Ste 84', 'Nowhere', 'AZ', '85702')
        assert person.job == new Job(id: 99, title: 'Big Muckemuck')
        assert person.pets[0] == new Pet(id: 22, name: 'Fluffy', animal: Animal.CAT)
        assert person.pets[1] == new Pet(id: 23, name: 'Rover', animal: Animal.DOG)
    }
}
