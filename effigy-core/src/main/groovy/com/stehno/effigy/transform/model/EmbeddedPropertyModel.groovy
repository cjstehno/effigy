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

package com.stehno.effigy.transform.model

import groovy.transform.Immutable
import org.codehaus.groovy.ast.ClassNode

/**
 * Entity property model representing an Embedded component.
 */
@Immutable(knownImmutableClasses = [ClassNode])
class EmbeddedPropertyModel implements EntityPropertyModel {

    String propertyName
    ClassNode type
    List<String> fieldNames
    List<String> columnNames
    List<Integer> columnTypes

    @SuppressWarnings('GroovyUnusedDeclaration')
    List collectSubProperties(Closure closure) {
        def list = []
        fieldNames.eachWithIndex { fn, i ->
            list << closure(fn, columnNames[i])
        }
        list
    }
}
