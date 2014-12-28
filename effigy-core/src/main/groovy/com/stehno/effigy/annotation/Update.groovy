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

import com.stehno.effigy.transform.AssociationSaveMethodInjector
import com.stehno.effigy.transform.UpdateTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*

/**
 * Annotation used to denote an "update" method in an Effigy repository.
 *
 * Update methods must accept an entity object or Map containing entity properties to be updated.
 *
 * Update methods must return one of the following:
 *  - void
 *  - a boolean denoting whether or not a change was made
 *  - an int value representing the number of entities
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [UpdateTransformer, AssociationSaveMethodInjector])
@interface Update {

    /**
     * Used to specify the selection criteria portion of the SQL using the Effigy SQL Template language. If no value is specified,
     * the method parameters will be used as entity properties to build a default criteria statement.
     */
    String value() default ''
}
