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

import com.stehno.effigy.annotation.*
import people.entity.Room

/**
 * Effigy-based implementation of the RoomRepository.
 */
@Repository(forEntity = Room)
abstract class EffigyRoomRepository implements RoomRepository {

    @Create
    abstract long create(Room room)

    @Create
    abstract long create(String name, int capacity)

    @Create
    abstract long create(Map<String, Object> map)

    @Delete
    abstract boolean delete(long id)

    @Delete
    abstract int deleteAll()

    @Delete
    abstract int deleteByCapacity(int capacity)

    @Delete('@capacity <= :min')
    abstract int deleteSmall(int min)

    @Count
    abstract int count(long id)

    @Count
    abstract int count()

    @Count('@capacity >= :min and @capacity <= :max')
    abstract int countByRange(int min, int max)

    @Exists
    abstract boolean exists(long id)

    @Exists
    abstract boolean exists()

    @Exists('@capacity >= :min and @capacity <= :max')
    abstract boolean exists(int min, int max)

    @Update
    abstract boolean update(Room room)

    @Update
    abstract int updateRoom(Map map)

    @Retrieve
    abstract List<Room> retrieveAll()

    @Retrieve
    abstract Room retrieve(long id)

    @Retrieve('#id = :entityId')
    abstract Room retrieveOne(long entityId)

    @Retrieve('@capacity <= :min')
    abstract List<Room> retrieveSmall(int min)

//    @Retrieve
//    abstract List<Room> retrieveMap(Map map)

    @Retrieve(order = '@capacity asc')
    abstract List<Room> retrieveAllOrdered()

    @Retrieve(limit = 2)
    abstract List<Room> retrieveLimited()

    @Retrieve(order = '@name asc')
    abstract List<Room> retrieveLimited(@Offset int offset, @Limit int limit)
}
