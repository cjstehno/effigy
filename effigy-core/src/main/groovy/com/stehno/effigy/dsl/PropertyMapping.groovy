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

import com.stehno.effigy.transform.util.StringUtils
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet

/**
 * Property mapping builder used by the RowMapper DSL.
 */
class PropertyMapping {

    private final String propertyName
    private String fieldName
    private Closure transformer
    private RowMapper rowMapper
    private String prefix

    PropertyMapping(String propertyName, String prefix) {
        this.propertyName = propertyName
        this.prefix = prefix
    }

    /**
     * Defines the database field name for the property mapping.
     *
     * @param fieldName the database field name
     * @return this property mapping instance
     */
    PropertyMapping from(String fieldName) {
        this.fieldName = fieldName
        this
    }

    /**
     * Defines a closure used to transform the database field value into the property value. The closure must take zero or one arguments (the field
     * value) and return the value to be stored in the property field.
     *
     * @param transformer the closure
     */
    void using(Closure transformer) {
        this.transformer = transformer
        this.rowMapper = null
    }

    /**
     * Allows a property to be extracted from the results of another row mapper.
     *
     * @param rowMapper the row mapper
     */
    void using(RowMapper rowMapper) {
        this.transformer = null
        this.rowMapper = rowMapper
    }

    void resolve(ResultSet rs, int rowNum, Object instance) {
        def value = rs.getObject("${prefix}${fieldName ?: StringUtils.camelCaseToUnderscore(propertyName)}")
        if (transformer) {
            instance[propertyName] = transformer.maximumNumberOfParameters == 1 ? transformer.call(value) : transformer.call()

        } else if (rowMapper) {
            instance[propertyName] = rowMapper.mapRow(rs, rowNum)

        } else {
            instance[propertyName] = value
        }

    }
}
