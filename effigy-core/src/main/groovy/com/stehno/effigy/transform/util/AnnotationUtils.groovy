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

package com.stehno.effigy.transform.util

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.Expression

/**
 * Utilities for working with annotations and AST transformations.
 */
class AnnotationUtils {

    // FIXME: most of these will only work with constant expressions - document and/or refactor

    static ClassNode extractClass(AnnotationNode annotation, String key) {
        def pair = annotation.members.find { pair -> pair.key == key }
        if (pair == null) {
            return null
        }
        return pair.value.type
    }

    static String extractString(AnnotationNode annotation, String key, String defvalue = null) {
        def pair = annotation.members.find { pair -> pair.key == key }
        return pair ? pair.value.value : defvalue
    }

    static boolean extractBoolean(AnnotationNode annotation, String key, boolean defvalue = false) {
        def pair = annotation.members.find { pair -> pair.key == key }
        return pair ? pair.value.value : defvalue
    }

    static Integer extractInteger(AnnotationNode annotation, String key, Integer defvalue = null) {
        def pair = annotation.members.find { pair -> pair.key == key }
        return pair ? pair.value.value : defvalue
    }

    static extractExpression(AnnotationNode node, String name, Expression defaultValue = null) {
        node?.getMember(name) ?: defaultValue
    }
}
