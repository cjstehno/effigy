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

        assert roomRepository.delete(idA)

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 0
    }

    @Test
    void createWithParams() {
        def idA = roomRepository.create('A', 10)
        def idB = roomRepository.create('B', 14)

        assert idA == 1
        assert idB == 2

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 2

        assert roomRepository.deleteAll() == 2

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 0
    }

    @Test
    void createWithMap() {
        def idA = roomRepository.create(name: 'A', capacity: 10)

        assert idA == 1

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 1
    }

    @Test
    void deleteCapacity() {
        roomRepository.create('A', 10)
        roomRepository.create('B', 14)
        roomRepository.create('C', 12)

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 3

        assert roomRepository.deleteByCapacity(12) == 1

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 2
    }

    @Test
    void deleteSmall() {
        roomRepository.create('A', 10)
        def id = roomRepository.create('B', 14)
        roomRepository.create('C', 12)

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 3

        assert roomRepository.exists()
        assert roomRepository.exists(id)
        assert !roomRepository.exists(23)
        assert roomRepository.exists(11, 15)
        assert !roomRepository.exists(2, 5)

        assert roomRepository.count() == 3
        assert roomRepository.count(id) == 1
        assert roomRepository.countByRange(11, 15) == 2
        assert roomRepository.countByRange(2, 5) == 0

        assert roomRepository.deleteSmall(13) == 2

        assert countRowsInTable(database.jdbcTemplate, 'rooms') == 1
    }

    @Test
    void updateWithEntity() {
        def idA = roomRepository.create(new Room(name: 'A', capacity: 10))

        assert idA
        assert roomRepository.count(idA) == 1
        assert roomRepository.exists(idA)

        assert roomRepository.update(new Room(id: idA, name: 'X', capacity: 100))

        assert roomRepository.countByRange(99, 101) == 1
    }

    @Test
    void updateWithMap() {
        def idA = roomRepository.create(new Room(name: 'A', capacity: 10))

        assert idA
        assert roomRepository.count(idA) == 1
        assert roomRepository.exists(idA)

        assert roomRepository.updateRoom(id: idA, name: 'X', capacity: 100) == 1

        assert roomRepository.countByRange(99, 101) == 1
    }

    @Test
    void retrieveAll() {
        roomRepository.create('A', 10)
        roomRepository.create('B', 14)
        roomRepository.create('C', 12)

        def rooms = roomRepository.retrieveAll()
        assert rooms.size() == 3

        def room = roomRepository.retrieve(2)
        assert room.id == 2
        assert room.name == 'B'
        assert room.capacity == 14

        def smalls = roomRepository.retrieveSmall(13)
        assert smalls.size() == 2

//        rooms = roomRepository.retrieveMap(name:'C', capacity: 12)
//        assert rooms.size() == 1

        rooms = roomRepository.retrieveAllOrdered()
        assert rooms.size() == 3
        assert rooms[0].name == 'A'
        assert rooms[1].name == 'C'
        assert rooms[2].name == 'B'

        rooms = roomRepository.retrieveLimited()
        assert rooms.size() == 2

        rooms = roomRepository.retrieveLimited(0, 1)
        assert rooms.size() == 1

        rooms = roomRepository.retrieveLimited(1, 2)
        assert rooms.size() == 2
    }
}
