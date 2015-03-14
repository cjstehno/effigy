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
 *  - no parameters to denote retrieving all entities
 *  - a Map object containing the properties corresponding to an entity object (String keys, Object values)
 *  - individual properties (by name and type) of the entity to be deleted
 *
 * Additionally the method may except one or more of the following (though only one of each):
 *  - an @Limit int property used to limit the query results returned
 *  - an @Offset int property used to offset the start of the returned results
 *
 * Create methods must return either:
 *  - a Collection (or extension of) of the entity managed by the repository
 *  - a Single instance of the entity managed by the repository
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
