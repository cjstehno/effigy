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

/**
 * Used to define a handler for reading/writing field data to and from the database.
 */
interface FieldTypeHandler<STORAGE, ENTITY> {

    /**
     * Reads the field data from the database and converts it to the specific type.
     *
     * @return the database field data converted to the specific type
     */
    ENTITY readField(STORAGE obj)

    /**
     * Writes the field data to the database by converting it from the specific type to the database type.
     *
     * @param T the value being written
     */
    STORAGE writeField(ENTITY obj)
}

class ClobFieldHandler implements FieldTypeHandler<Clob, byte[]> {

    @Override
    byte[] readField(Clob obj) {
        return new byte[0]
    }

    @Override
    Clob writeField(byte[] obj) {
        return null
    }
}