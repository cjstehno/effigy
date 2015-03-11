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

package com.stehno.effigy.annotation

import java.lang.annotation.*

/**
 * This annotation is used with a SqlSelect annotation to provide information about the RowMapper instance to be used.
 *
 * There are three distinct configuration scenarios for mapper annotations, based on the annotation property values:
 *
 * * the 'bean' property can be used to specify the name of a bean (implementing RowMapper) which will be autowired into the repository and used by
 *   the query.
 * * the 'type' property can be used to specify a class implementing RowMapper which will be instantiated as a shared instance and used by the query.
 * * the 'type' and 'factory' properties may be used similar to the 'type' property alone, except that the specified static factory method will be
 *   called to create the instance, rather than the constructor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface RowMapper {

    /**
     * The name of the bean to be autowired into the repository for use as a RowMapper instance. If the default value is used, this property will
     * be ignored.
     */
    String bean() default ''

    /**
     * The class implementing RowMapper that is to be used by the query. If the default value is used, this property will be ignored.
     */
    Class type() default Void.class

    /**
     * The name of a static factory method on the class provided by the 'type' property. This property is ignored unless a 'type' value is specified.
     */
    String factory() default ''

    // FIXME: document here and user guide
    boolean singleton() default true
}