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

import org.springframework.jdbc.core.RowMapper

/**
 * Builder used to create RowMapper instances based on the RowMapper DSL.
 * This API is not intended for direct use, but rather through the DSL.
 *
 * <pre>RowMapper<InterestingObject> rowMapper = mapper(InterestingObject) {*       map 'partName'
 *       map 'someDate' using { x -> new Date(x) }*       map 'items' from 'line_items' using { x -> x.split(';') }*       map 'lineNumber' from 'line_number'
 *       map 'something' using mapper(EmbedddObject, 'obj_') {*           map 'id'
 *           map 'label'
 *}*}</pre>
 */
class RowMapperBuilder<T> {

    private final Class<? extends T> mappedType
    private final String prefix
    private final List<PropertyMapping> mappings = []

    private RowMapperBuilder(final Class<? extends T> mappedType, final String prefix) {
        this.mappedType = mappedType
        this.prefix = prefix
    }

    /**
     * Adds a mapping of the specified property. If no "from" is specified, the property name will be converted to underscore_case.
     *
     * @param propertyName the mapped property name
     * @return an instance of the PropertyMapping object (associated with this builder)
     */
    PropertyMapping map(String propertyName) {
        def mapping = new PropertyMapping(propertyName, prefix)
        mappings << mapping
        mapping
    }

    /**
     * Creates a RowMapper instance from the mappings configured in the DSL closure.
     *
     * @param mappedType the type to be mapped
     * @param prefix the field name prefix to use (if any)
     * @param closure the DSL closure
     * @return the generated mapper
     */
    static <T> RowMapper<T> mapper(Class<? extends T> mappedType, String prefix = '', Closure closure) {
        def builder = new RowMapperBuilder<T>(mappedType, prefix)
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure()
        builder.build()
    }

    private RowMapper<T> build() {
        new DynamicRowMapper<T>(mappedType, mappings.asImmutable())
    }
}

