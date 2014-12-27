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

import com.stehno.effigy.annotation.Create
import com.stehno.effigy.annotation.Delete
import com.stehno.effigy.annotation.Repository
import people.entity.Room

/**
 * Effigy-based implementation of the RoomRepository.
 */
@Repository(forEntity = Room)
abstract class EffigyRoomRepository implements RoomRepository {

    @Override @Create
    abstract long create(Room room)

    @Override @Create
    abstract long create(String name, int capacity)

    @Override @Create
    abstract long create(Map<String,Object> map)

    @Override @Delete
    abstract boolean delete(long id)

    @Override @Delete
    abstract int deleteAll()
}
