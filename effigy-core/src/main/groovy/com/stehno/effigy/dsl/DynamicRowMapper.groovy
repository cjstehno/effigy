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

package com.stehno.effigy.dsl

import groovy.transform.Immutable
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

/**
 * RowMapper class used by the RowMapper DSL to contain the configured mappings.
 */
@Immutable
class DynamicRowMapper<T> implements RowMapper<T> {

    /**
     * The type provided by the mapper.
     */
    Class<? extends T> mappedType

    /**
     * The list of property mappings objects from the DSL.
     */
    List<PropertyMapping> mappings

    @Override
    T mapRow(ResultSet rs, int rowNum) throws SQLException {
        def instance = mappedType.newInstance()
        mappings*.resolve(rs, rowNum, instance)
        instance
    }
}
