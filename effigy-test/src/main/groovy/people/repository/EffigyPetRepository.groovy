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

import com.stehno.effigy.annotation.Count
import com.stehno.effigy.annotation.Create
import com.stehno.effigy.annotation.Repository
import com.stehno.effigy.annotation.Retrieve
import people.entity.Pet

/**
 * Created by cjstehno on 1/3/15.
 */
@Repository(Pet)
abstract class EffigyPetRepository implements PetRepository {

    @Create
    abstract Long create(Map map)

    @Retrieve
    abstract Pet retrieve(Long id)

    @Count
    abstract int count(Long id)

    @Count
    abstract int countAll()
}
