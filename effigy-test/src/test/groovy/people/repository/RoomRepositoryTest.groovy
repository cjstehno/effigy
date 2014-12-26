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

import com.stehno.effigy.transform.util.JdbcTemplateHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.test.jdbc.JdbcTestUtils
import people.DatabaseEnvironment
import people.entity.Room

import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable

class RoomRepositoryTest {

    @Rule
    public DatabaseEnvironment database = new DatabaseEnvironment()

    private RoomRepository roomRepository

    @Before
    void before() {
        roomRepository = new EffigyRoomRepository(jdbcTemplate: database.jdbcTemplate)
    }

    @Test
    void createWithEntity() {
        def idA = roomRepository.create(new Room(name: 'A', capacity: 10))

        assert idA == 1

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 1
    }

    @Test
    void createWithParams() {
        def idA = roomRepository.create('A', 10)

        assert idA == 1

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 1
    }

    @Test
    void createWithMap() {
        def idA = roomRepository.create(name: 'A', capacity: 10)

        assert idA == 1

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 1
    }
}
