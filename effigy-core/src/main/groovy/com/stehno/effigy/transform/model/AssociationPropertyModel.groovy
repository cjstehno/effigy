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

package com.stehno.effigy.transform.model

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassNode

/**
 * Entity property model object for associations.
 */
@Immutable(knownImmutableClasses = [ClassNode]) @CompileStatic
class AssociationPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type
    ClassNode associatedType

    String joinTable
    String entityColumn
    String assocColumn

    // TODO: is there a better place for this?
    String mapKeyProperty
}
