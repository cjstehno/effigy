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
 * Annotation used to denote a delete method in an Effigy repository.
 *
 * Delete method must accept as parameters, one of the following:
 *  - no parameters to denote deleting all entities
 *  - a Map object containing the properties corresponding to an entity object (String keys, Object values)
 *  - individual properties (by name and type) of the entity to be deleted
 *
 * Create methods must return either:
 *  - a boolean denoting whether or not something was actually deleted
 *  - an int denoting the number of items deleted
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@GroovyASTTransformationClass(classes = [DeleteTransformer])
@interface Delete {

    /**
     * Used to specify the deletion criteria portion of the SQL using the Effigy SQL Template language. If no value is specified,
     * the method parameters will be used as entity properties to build a default criteria statement.
     */
    String value() default ''
}