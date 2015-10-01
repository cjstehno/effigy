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

import com.stehno.effigy.JdbcStrategy
import groovy.transform.Immutable
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression

import static com.stehno.effigy.transform.util.AnnotationUtils.extractAnnotation
import static com.stehno.effigy.transform.util.AnnotationUtils.extractEnum
import static org.codehaus.groovy.transform.AbstractASTTransformation.getMemberStringValue

/**
 * Created by cjstehno on 9/29/15.
 */
@Immutable(knownImmutables = ['mapper', 'extractor'])
class SqlSelectModel {
    // TODO: rename this to **Annotation or **Config?

    String sql
    String source
    JdbcStrategy strategy
    AnnotationConstantExpression mapper
    // FIXME: change this to a model
    AnnotationConstantExpression extractor
    // FIXME: change this to a model

    // FIXME: this should build a useful model for working with the annotation data
    static SqlSelectModel extract(AnnotationNode node) {
        new SqlSelectModel(
            getMemberStringValue(node, 'value'),
            getMemberStringValue(node, 'source'),
            extractEnum(node, 'strategy', JdbcStrategy.GROOVY),
            extractAnnotation(node, 'mapper', null),
            extractAnnotation(node, 'extractor', null)
        )
    }
}
