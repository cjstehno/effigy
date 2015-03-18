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

import groovy.transform.Immutable
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

import static com.stehno.effigy.transform.util.StringUtils.camelCaseToUnderscore

/**
 * Created by cjstehno on 3/15/15.
 */
class RowMapperBuilder<T> {
    // FIXME: API documentation
    // FIXME: user guide documentation

    private final Class<? extends T> mappedType
    private final String prefix
    private final List<PropertyMapping> mappings = []

    private RowMapperBuilder(final Class<? extends T> mappedType, final String prefix) {
        this.mappedType = mappedType
        this.prefix = prefix
    }

    PropertyMapping map(String propertyName) {
        def mapping = new PropertyMapping(propertyName, prefix)
        mappings << mapping
        mapping
    }

    static <T> RowMapper<T> mapper(Class<? extends T> mappedType, String prefix = '', Closure closure) {
        def builder = new RowMapperBuilder<T>(mappedType, prefix)
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure()
        builder.build()
    }

    RowMapper<T> build() {
        new DynamicRowMapper<T>(mappedType, mappings.asImmutable())
    }
}

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

    PropertyMapping from(String fieldName) {
        this.fieldName = fieldName
        this
    }

    void using(Closure transformer) {
        this.transformer = transformer
        this.rowMapper = null
    }

    void using(RowMapper rowMapper) {
        this.transformer = null
        this.rowMapper = rowMapper
    }

    // TODO: should I store desired type (or resolve it)
    void resolve(ResultSet rs, int rowNum, Object instance) {
        def value = rs.getObject("${prefix}${fieldName ?: camelCaseToUnderscore(propertyName)}")
        if (transformer) {
            instance[propertyName] = transformer.maximumNumberOfParameters == 1 ? transformer.call(value) : transformer.call()

        } else if (rowMapper) {
            instance[propertyName] = rowMapper.mapRow(rs, rowNum)

        } else {
            instance[propertyName] = value
        }

    }
}


// TODO: this wont handle immutable pogos...

@Immutable
class DynamicRowMapper<T> implements RowMapper<T> {

    Class<? extends T> mappedType
    List<PropertyMapping> mappings

    @Override
    T mapRow(ResultSet rs, int rowNum) throws SQLException {
        def instance = mappedType.newInstance()
        mappings*.resolve(rs, rowNum, instance)
        instance
    }
}