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

package com.stehno.effigy.transform.sql

import org.codehaus.groovy.ast.expr.Expression
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import static com.stehno.effigy.transform.sql.RawSqlBuilder.rawSql
import static org.mockito.Mockito.mock

@RunWith(MockitoJUnitRunner)
class RawSqlBuilderTest {

    @Test void 'basic usage'() {
        def builder = rawSql('select a,b,c from foo where d = :x and e > :y and f = :x')

        def expX = mock(Expression, 'x')
        def expY = mock(Expression, 'y')

        builder.param('x', expX)
        builder.param('y', expY)

        assert builder.build() == 'select a,b,c from foo where d = ? and e > ? and f = ?'
        assert builder.params.size() == 3
        assert builder.params[0] == expX
        assert builder.params[1] == expY
        assert builder.params[2] == expX
    }

    @Test void 'usage with no replacements'() {
        def builder = rawSql('select distinct(last_name) from people')

        assert builder.build() == 'select distinct(last_name) from people'
        assert builder.params.isEmpty()
    }

    @Test(expected = IllegalArgumentException) void 'usage with error'() {
        rawSql('select a,b,c from foo where d = :x and e > :y and f = :x').params
    }
}
