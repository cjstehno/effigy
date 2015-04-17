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

package com.stehno.effigy.jdbc

import java.sql.Clob
import java.sql.ResultSet

/**
 * Used to define a handler for reading/writing field data to and from the database.
 */
interface FieldTypeHandler<T> {

    /**
     * Reads the field data from the database and converts it to the specific type.
     *
     * @return the database field data converted to the specific type
     */
    T readField(ResultSet rs, String fieldName)

    /**
     * Writes the field data to the database by converting it from the specific type to the database type.
     *
     * @param T the value being written
     */
    void writeField(T obj)
}

class ClobFieldHandler implements FieldTypeHandler<Clob> {

    @Override
    Clob readField(ResultSet rs, String fieldName) {
        return null
    }

    @Override
    void writeField(Clob obj) {

    }
}