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

import people.entity.Room

/**
 * Created by cjstehno on 12/26/14.
 */
interface RoomRepository {

    long create(Room room)

    long create(String name, int capacity)

    long create(Map<String,Object> map)

    boolean delete(long id)

    int deleteAll()

    int deleteByCapacity(int capacity)

    int deleteSmall(int min)

    int count(long id)

    int count()

    int countByRange(int min, int max)

    boolean exists(long id)

    boolean exists()

    boolean exists(int min, int max)

    boolean update(Room room)

    int updateRoom(Map map)

    List<Room> retrieveAll()

    Room retrieve(long id)

    List<Room> retrieveSmall(int min)

//    List<Room> retrieveMap(Map map)

    List<Room> retrieveAllOrdered()

    List<Room> retrieveLimited()

    List<Room> retrieveLimited(int offset, int limit)
}