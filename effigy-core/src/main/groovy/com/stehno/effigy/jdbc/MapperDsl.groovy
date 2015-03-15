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

/**
 * Created by cjstehno on 3/15/15.
 */
class MapperDsl {

    static <T> RowMapper<T> mapper(Class<? extends T> mappedType, Closure closure) {
        def builder = new RowMapperBuilder<T>(mappedType)
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure()
        builder.build()
    }
}

class RowMapperBuilder<T> {

    private final Class<? extends T> mappedType
    private final List<Mapping> mappings = []

    RowMapperBuilder(Class<? extends T> mappedType) {
        this.mappedType = mappedType
    }

    Mapping map(String fieldName) {
        def mapping = new Mapping(fieldName)
        mappings << mapping
        mapping
    }

    Mapping mapProperty(String propertyName) {
        def mapping = new Mapping(fieldize(propertyName), propertyName)
        mappings << mapping
        mapping
    }

    RowMapper<T> build() {
        new DynamicRowMapper<T>(mappedType, mappings.asImmutable())
    }

    private static fieldize(String name) {
        // FIXME: should convert camel-case to underscore
        name
    }
}

class Mapping {

    private final String fieldName
    private String propertyName
    private Closure transformer

    Mapping(String fieldName) {
        this.fieldName = fieldName
    }

    Mapping(String fieldName, String propertyName) {
        this(fieldName)
        this.propertyName = propertyName
    }

    void into(String propertyName) {
        this.propertyName = propertyName
    }

    Mapping using(Closure transformer) {
        this.transformer = transformer
        this
    }

    // TODO: should I store desired type (or resolve it)
    void resolve(ResultSet rs, Object instance) {
        def value = rs.getObject(fieldName)
        instance[propertyName] = transformer ? transformer.call(value) : value
    }
}

// TODO: this wont handle immutable pogos...

@Immutable
class DynamicRowMapper<T> implements RowMapper<T> {

    Class<? extends T> mappedType
    List<Mapping> mappings

    @Override
    T mapRow(ResultSet rs, int rowNum) throws SQLException {
        def instance = mappedType.newInstance()
        mappings*.resolve(rs, instance)
        instance
    }
}