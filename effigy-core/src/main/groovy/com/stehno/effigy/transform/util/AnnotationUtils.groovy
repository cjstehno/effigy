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

import groovy.transform.TypeChecked
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression

import static groovy.transform.TypeCheckingMode.SKIP

/**
 * Created by cjstehno on 9/29/15.
 */
@TypeChecked
class AnnotationUtils {

    static AnnotationConstantExpression extractAnnotation(AnnotationNode annotation, String name) {
        def member = annotation.members[name] as AnnotationConstantExpression
        if (member) {
            return member
        } else {
            throw new IllegalArgumentException("Annotation member (String $name) has no specified value.")
        }
    }

    static AnnotationConstantExpression extractAnnotation(AnnotationNode annotation, String name, AnnotationConstantExpression defaultValue) {
        def member = annotation.members[name] as AnnotationConstantExpression
        return member ?: defaultValue
    }

    @TypeChecked(SKIP)
    static <E> E extractEnum(AnnotationNode annotation, String name, E defaultValue) {
        def member = annotation.members[name] as PropertyExpression
        return member?.property?.text ? defaultValue.class.valueOf(member.property.text) : defaultValue
    }
}
