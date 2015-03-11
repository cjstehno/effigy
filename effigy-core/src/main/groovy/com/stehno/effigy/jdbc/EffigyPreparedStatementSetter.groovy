package com.stehno.effigy.jdbc

import org.springframework.jdbc.core.PreparedStatementSetter

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Effigy-specific implementation of the PreparedStatementSetter interface used to provide access to the method arguments. A method annotated with
 * @PreparedStatementSetter using an implementation of this abstract class will be able to access the method arguments at runtime for use within the
 * setter.
 */
abstract class EffigyPreparedStatementSetter implements PreparedStatementSetter {

    // FIXME: document me in user guide
    /*
        FIXME:
            should not allow autowired beans of this PSS type - since its not stateless.
            add property to PSS annotation - singleton=true|false
                - singleton is default
                - false is prototype which will create a new instancce for each call
                - just roll this into the other helpers too, might be useful to allow stateful
                - bean-defined helpers should not allow prototype=true - unless I want to add in appliccationcontext fetcchingt

            this type is always prototype (and checked)
            instance will be created and have arguments of mthod provide to it
     */

    Map<String,Object> methodArguments = [:]

    /**
     * Delegates to the setValues(PreparedStatement, Map<String,Object>) version of the method, with an empty map instance.
     */
    @Override
    final void setValues(PreparedStatement ps) throws SQLException {
        setValues(ps, methodArguments)
    }

    /**
     * This method should be implemented to set the desired values on the provided PreparedStatement instance. The arguments map may or may not be
     * populated depending on how this setter is configured.
     *
     * @param ps the prepared statement provided by the framework
     * @param arguments the method runtime arguments
     */
    abstract void setValues(PreparedStatement ps, Map<String,Object> arguments)
}