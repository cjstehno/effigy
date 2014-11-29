package com.stehno.effigy.jdbc

import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Used internally as a basis for the generated Effigy entity row mappers.
 */
abstract class EffigyEntityRowMapper<E> implements RowMapper<E> {

    String prefix = ''

    @Override
    E mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        E entity = newEntity()
        mapping(rs, entity)
        entity
    }

    abstract protected E newEntity()

    abstract protected void mapping( ResultSet rs, E entity )
}
