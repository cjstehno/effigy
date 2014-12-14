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
import org.springframework.jdbc.core.RowMapper
import people.entity.Address
import people.entity.Person

import java.sql.ResultSet

@RunWith(MockitoJUnitRunner)
class PersonRowMapperTest {

    @Mock private ResultSet resultSet

    @Test void 'mapping: default prefix'() {
        def now = new Date()

        when(resultSet.getObject('id')).thenReturn(123L)
        when(resultSet.getObject('version')).thenReturn(2L)
        when(resultSet.getObject('first_name')).thenReturn('John')
        when(resultSet.getObject('middle_name')).thenReturn('Q')
        when(resultSet.getObject('last_name')).thenReturn('Public')
        when(resultSet.getObject('date_of_birth')).thenReturn(now)
        when(resultSet.getObject('married')).thenReturn(true)
        when(resultSet.getObject('home_line1')).thenReturn('123 W East St')
        when(resultSet.getObject('home_line2')).thenReturn('Apt 42')
        when(resultSet.getObject('home_city')).thenReturn('Somewhere')
        when(resultSet.getObject('home_state')).thenReturn('AZ')
        when(resultSet.getObject('home_zip')).thenReturn('85701')

        RowMapper<Person> mapper = Person.rowMapper()
        Person person = mapper.mapRow(resultSet, 0)

        assert person.id == 123L
        assert person.version == 2L
        assert person.firstName == 'John'
        assert person.middleName == 'Q'
        assert person.lastName == 'Public'
        assert person.birthDate == now
        assert person.married
        assert person.home == new Address('123 W East St', 'Apt 42', 'Somewhere', 'AZ', '85701')
    }

    @Test void 'mapping: specified prefix'() {
        def now = new Date()

        when(resultSet.getObject('foo_id')).thenReturn(123L)
        when(resultSet.getObject('foo_version')).thenReturn(2L)
        when(resultSet.getObject('foo_first_name')).thenReturn('John')
        when(resultSet.getObject('foo_middle_name')).thenReturn('Q')
        when(resultSet.getObject('foo_last_name')).thenReturn('Public')
        when(resultSet.getObject('foo_date_of_birth')).thenReturn(now)
        when(resultSet.getObject('foo_married')).thenReturn(true)
        when(resultSet.getObject('foo_home_line1')).thenReturn('123 W East St')
        when(resultSet.getObject('foo_home_line2')).thenReturn('Apt 42')
        when(resultSet.getObject('foo_home_city')).thenReturn('Somewhere')
        when(resultSet.getObject('foo_home_state')).thenReturn('AZ')
        when(resultSet.getObject('foo_home_zip')).thenReturn('85701')

        RowMapper<Person> mapper = Person.rowMapper('foo_')
        Person person = mapper.mapRow(resultSet, 0)

        assert person.id == 123L
        assert person.version == 2L
        assert person.firstName == 'John'
        assert person.middleName == 'Q'
        assert person.lastName == 'Public'
        assert person.birthDate == now
        assert person.married
        assert person.home == new Address('123 W East St', 'Apt 42', 'Somewhere', 'AZ', '85701')
    }
}
