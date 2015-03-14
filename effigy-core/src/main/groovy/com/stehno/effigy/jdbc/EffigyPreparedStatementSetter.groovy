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

import org.springframework.jdbc.core.PreparedStatementSetter

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Effigy-specific implementation of the PreparedStatementSetter interface used to provide access to the method arguments. A method annotated with
 * @PreparedStatementSetter using an implementation of this abstract class will be able to access the method arguments at runtime for use within the
 * setter.
 */
abstract class EffigyPreparedStatementSetter implements PreparedStatementSetter, ArgumentAwareHelper {

    /**
     * The arguments passed into the method. The key will be the argument name and the value will be the argument value.
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

