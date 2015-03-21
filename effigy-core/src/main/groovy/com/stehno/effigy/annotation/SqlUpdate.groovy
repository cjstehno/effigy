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

package com.stehno.effigy.annotation

import java.lang.annotation.*

/**
 * Annotation used to denote a custom SQL-based update method in an Effigy repository.
 *
 * An "update" method may accept any type or primitive as input parameters; however, the name of the parameter will
 * used as the name of the replacement variable in the SQL statement, so they will need to be consistent.
 *
 * An "update" method must have a return type of one of the following:
 * <ul>
 *     <li>void</li>
 *     <li>a boolean denoting a non-zero update record count (true) or 0 (false)</li>
 *     <li>an int or long denoting the updated record count.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface SqlUpdate {

    /**
     * Used to provide the SQL string which will be compiled into the method. The method parameters will be used as replacement variables in the SQL
     * using the parameter name prefixed with a colon (e.g. `:firstName`).
     */
    String value()
}