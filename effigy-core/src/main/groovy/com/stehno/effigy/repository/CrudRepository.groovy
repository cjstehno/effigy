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

package com.stehno.effigy.repository

import com.stehno.effigy.annotation.*

/**
 *  Effigy-specific implementation of the CrudOperations interface. It uses the Effigy annotations to build the common CRUD
 *  operation implementations.
 */
abstract class CrudRepository<E, K> implements CrudOperations<E, K> {

    @Create
    abstract K create(E entity)

    @Retrieve('#id = :entityId')
    abstract E retrieve(K entityId)

    @Retrieve
    abstract List<E> retrieveAll()

    @Update
    abstract void update(E entity)

    @Delete('#id = :entityId')
    abstract boolean delete(K entityId)

    @Delete
    abstract boolean deleteAll()

    @Count
    abstract int count()

    @Exists('#id = :entityId')
    abstract boolean exists(K entityId)
}
