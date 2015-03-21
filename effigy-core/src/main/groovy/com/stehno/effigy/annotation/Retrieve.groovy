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
 * Annotation used to denote a "retrieve" method in an Effigy repository.
 *
 * Retrieve methods must accept as parameters, one of the following:
 * <ul>
 *     <li>no parameters to denote retrieving all entities</li>
 *     <li>a Map object containing the properties corresponding to an entity object (String keys, Object values)</li>
 *     <li>individual properties (by name and type) of the entity to be deleted</li>
 * </ul>
 *
 * Additionally the method may except one or more of the following (though only one of each):
 * <ul>
 *     <li>an <code>@Limit</code> int property used to limit the query results returned</li>
 *     <li>an <code>@Offset</code> int property used to offset the start of the returned results</li>
 * </ul>
 *
 * Create methods must return either:
 * <ul>
 *     <li>a Collection (or extension of) of the entity managed by the repository</li>
 *     <li>a Single instance of the entity managed by the repository</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Retrieve {

    /**
     * Used to specify the retrieval criteria portion of the SQL using the Effigy SQL Template language. If no value is specified,
     * the method parameters will be used as entity properties to build a default criteria statement.
     */
    String value() default ''

    /**
     * The select offset value to be used. If this attribute is specified, any PageBy or LimitBy value passed into the method as
     * parameters will be ignored.
     *
     * The default value provides no specific offset.
     */
    int offset() default -1

    /**
     * The select limit value to be used. If this attribute is specified, any PageBy or LimitBy value passed into the method as
     * parameters will be ignored.
     *
     * The default value provides no specific limit.
     */
    int limit() default -1

    /**
     * The ordering Sql Template to be used by the query. The default provides no specific ordering.
     */
    String order() default ''
}
