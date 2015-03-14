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

package com.stehno.effigy.transform.sql

import org.codehaus.groovy.ast.expr.Expression

/**
 * Trait used to add predication-support to the Sql builders that require it.
 */
trait Predicated<T> {

    private final _wheres = []
    private final _params = []

    List<String> getWheres() { return _wheres.asImmutable() }

    T wheres(List<String> criteria, List<Expression> paramXs) {
        if (criteria) {
            _wheres.addAll(criteria)
            _params.addAll(paramXs)
        }
        this
    }

    T where(String criteria, List<Expression> paramXs) {
        if (criteria) {
            _wheres.add(criteria)
            _params.addAll(paramXs)
        }
        this
    }

    T where(String criteria, Expression paramX) {
        _wheres << criteria
        _params << paramX
        this
    }

    List<Expression> getWhereParams() { _params.asImmutable() }

    List<Expression> getParams() { getWhereParams() }
}
