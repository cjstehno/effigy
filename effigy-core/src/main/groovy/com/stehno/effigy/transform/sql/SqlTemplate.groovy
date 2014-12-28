/*
 * Copyright (c) 2014 Christopher J. Stehno
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

package com.stehno.effigy.transform.sql

import org.codehaus.groovy.ast.ClassNode

import static com.stehno.effigy.transform.model.EntityModel.entityProperty

/**
 * Created by cjstehno on 12/27/14.
 */
class SqlTemplate {

    public static final VARIABLE_PATTERN = /:[A-Za-z0-9]*/
    public static final PROPERTY_PATTERN = /@[A-Za-z0-9]*/
    final String text

    SqlTemplate(final String text) {
        this.text = text
    }

    /**
     * Retrieves the property names from the string (the @name values).
     *
     * return a Set of property names (including the @ prefix)
     */
    Set<String> propertyNames() {
        text.findAll(PROPERTY_PATTERN).unique()
    }

    /**
     * Retrieves the variable replacement names from the string (the :name values).
     *
     * @return a List of the variable names (including the : prefix)
     */
    List<String> variableNames() {
        text.findAll(VARIABLE_PATTERN)
    }

    /**
     * Converts the Sql template text into valid a valid SQL fragment based on the provided entity node. The
     * properties are converted to the proper column names and the replacement variables are converted to proper
     * JDBC placeholders (?).
     *
     * e.g.
     *
     *  @firstName = :firstName
     *
     * -becomes-
     *
     *  first_name = ?
     *
     * @param entityNode the entity node to be referenced in the SQL fragment
     * @return the converted SQL fragment
     */
    @SuppressWarnings('GroovyAssignabilityCheck')
    String sql(ClassNode entityNode) {
        // replace @ with col name and : with ?
        String sql = text.replaceAll(VARIABLE_PATTERN, '?')

        propertyNames().each { String pname ->
            sql = sql.replaceAll(pname, entityProperty(entityNode, pname[1..-1]).columnName)
        }

        sql
    }
}
