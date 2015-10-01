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

package com.stehno.effigy.transform

import spock.lang.Ignore
import spock.lang.Specification

import javax.sql.DataSource

class SqlSelectTransformSpec extends Specification {

    GroovyShell shell = new GroovyShell()

    @Ignore
    def 'basic testing'() {
        setup:
        def repository = shell.evaluate '''
            import com.stehno.effigy.JdbcStrategy
            import com.stehno.effigy.annotation.RowMapper
            import com.stehno.effigy.annotation.SqlSelect

            import javax.sql.DataSource

            class SomeRepository {

                DataSource dataSource

                @SqlSelect('select a,b from somewhere where c=:c')
                Collection findFoos(int c){}
            }

            new SomeRepository()
        '''

        repository.dataSource = Mock(DataSource)

        when:
        def values = repository.findFoos(3)

        then:
        values
    }
}
