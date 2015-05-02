/*
 * Copyright (c) 2015 Christopher J. Stehno
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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet

import static org.mockito.Mockito.when
import static people.entity.Animal.DOG

@RunWith(MockitoJUnitRunner)
class PetRowMapperTest {

    @Mock private ResultSet resultSet
    private RowMapper<Pet> mapper

    @Before void before() {
        mapper = Pet.rowMapper('')
    }

    @Test void 'rowMapper: simple'() {
        setupResultSet 2468, 'Fluffy', 'DOG'

        Pet pet = mapper.mapRow(resultSet, 1)
        assert pet
        assert new Pet(id: 2468L, name: 'Fluffy', animal: DOG)
    }

    @Test void 'rowMapper: all empty'() {
        setupResultSet null, null, null

        Pet pet = mapper.mapRow(resultSet, 1)
        assert !pet
    }

    @Test void 'rowMapper: empty id'() {
        setupResultSet null, 'Squeaky', 'CAT'

        Pet pet = mapper.mapRow(resultSet, 1)
        assert !pet
    }

    private void setupResultSet(Long id, String name, String animal) {
        when(resultSet.getObject('id')).thenReturn(id)
        when(resultSet.getObject('name')).thenReturn(name)
        when(resultSet.getObject('animal')).thenReturn(animal)
    }
}