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
import people.entity.Animal
import people.entity.Pet

import java.sql.ResultSet

@RunWith(MockitoJUnitRunner)
class PetRowMapperTest {

    @Mock private ResultSet resultSet

    @Test void 'mapper: default prefix'() {
        when(resultSet.getObject('id')).thenReturn(123L)
        when(resultSet.getObject('name')).thenReturn('Fluffy')
        when(resultSet.getObject('animal')).thenReturn('CAT')

        RowMapper<Pet> mapper = Pet.rowMapper()
        Pet pet = mapper.mapRow(resultSet, 0)

        assert pet.id == 123
        assert pet.name == 'Fluffy'
        assert pet.animal == Animal.CAT
    }

    @Test void 'mapper: different prefix'() {
        when(resultSet.getObject('foo_id')).thenReturn(123L)
        when(resultSet.getObject('foo_name')).thenReturn('Fluffy')
        when(resultSet.getObject('foo_animal')).thenReturn('CAT')

        RowMapper<Pet> mapper = Pet.rowMapper('foo_')
        Pet pet = mapper.mapRow(resultSet, 0)

        assert pet.id == 123
        assert pet.name == 'Fluffy'
        assert pet.animal == Animal.CAT
    }
}
