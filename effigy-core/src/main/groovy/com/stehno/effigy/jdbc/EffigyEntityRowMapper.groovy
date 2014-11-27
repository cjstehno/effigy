package com.stehno.effigy.jdbc

import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by cjstehno on 11/27/2014.
 */
class EffigyEntityRowMapper<E> implements RowMapper<E> {

    private final Map<String,String> mappings // property : column
    private final Class<E> entityType

    EffigyEntityRowMapper(final Class<E> entityType, final Map<String,String> mappings) {
        this.entityType = entityType
        this.mappings = mappings
    }

    @Override
    E mapRow(ResultSet rs, int rowNum) throws SQLException {
        E entity = entityType.newInstance()
        mappings.each { p,c ->
            entity[p] = rs.getObject(c)
        }
        entity
    }
}
