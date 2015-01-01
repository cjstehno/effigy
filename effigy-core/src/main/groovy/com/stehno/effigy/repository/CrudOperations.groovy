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

/**
 *  Helper interface useful for providing the common CRUD operations. This interface is not Effigy-specific so that it may be used
 *  as part of the top-level interface definition for your repositories regardless of the underlying implementation.
 */
interface CrudOperations<E, K> {

    /**
     * Persists the new entity in the database. The ID value will be returned and also updated on the entity instance passed into the method.
     * An exception should be thrown if the entity could not be persisted.
     *
     * @param entity the entity to be saved
     * @return the id of the saved entity
     */
    K create(E entity)

    /**
     * Retrieves an existing entity with the given id. If no entity with the specified id exists, an exception will be thrown.
     *
     * @param entityId the id of the entity to be retrieved
     * @return the entity
     */
    E retrieve(K entityId)

    /**
     * Retrieves all of the instances of the managed entity.
     *
     * @return a List of all the entities in the database for the entity type.
     */
    List<E> retrieveAll()

    /**
     * Updates the existing entity in the database. If the entity does not exist, an exception will be thrown. If the entity has a property annotated
     * with the @Version annotation, the version must match that stored in the database, or an exception will be thrown.
     *
     * @param entity the entity to be updated
     */
    void update(E entity)

    /**
     * Deletes the entity with the given id. If no entity exists with the given id, no action will be taken. A value of true or false
     * will be returned based on whether or not an entity was actually deleted.
     *
     * If the entity has associations, the references to the associated entities will be removed, not any of the associated objects themselves.
     *
     * @param entityId the entity id
     * @return a value of true if an entity was actually deleted
     */
    boolean delete(K entityId)

    /**
     * Deletes all of the entities of the type from the database, and clears out any relation references.
     *
     * @return a value of true, if any entities were actually deleted
     */
    boolean deleteAll()

    /**
     * Retrieves a count of all the target entities in the database.
     *
     * @return a count of the entities of the entity type in the database
     */
    int count()

    /**
     * Used to determine whether or not an entity exists of the target type with the specified id.
     *
     * @param entityId the id of the entity to be verified
     * @return true if the entity exists
     */
    boolean exists(K entityId)
}