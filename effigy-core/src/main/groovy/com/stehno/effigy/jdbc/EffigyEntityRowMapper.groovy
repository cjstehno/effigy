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

import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Used internally as a basis for the generated Effigy entity row mappers.
 */
abstract class EffigyEntityRowMapper<E> implements RowMapper<E> {

    /**
     * The column name prefix to be used in column data retrieval. Defaults to an empty string.
     */
    String prefix = ''

    @Override
    E mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        mapping(rs, newEntity())
    }

    abstract protected E newEntity()

    abstract protected E mapping(ResultSet rs, E entity)
}
