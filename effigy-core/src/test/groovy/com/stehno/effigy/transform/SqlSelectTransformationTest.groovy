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

package com.stehno.effigy.transform

import com.stehno.effigy.test.ClassAssertions
import com.stehno.effigy.test.ClassBuilder
import com.stehno.effigy.test.DatabaseEnvironment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

import static com.stehno.effigy.test.ClassAssertions.forObject
import static com.stehno.effigy.test.ClassBuilder.forCode

class SqlSelectTransformationTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment(schema: 'schema-a.sql', data: 'data-a.sql')

    private final ClassBuilder repoBuilder = forCode('''
        package testing

        import com.stehno.effigy.annotation.*

        @Repository
        abstract class SelectorTestRepository {
            $code
        }
    ''')

    @Before void before() {
        repoBuilder.reset()
    }

    @Test void 'int countItems(int min, int max)'() {
        repoBuilder.inject('''
            @SqlSelect('select count(*) from someone where age > :min and age < :max')
            abstract int countItems(int min, int max)
        ''')

        def repo = repoBuilder.instantiate()

        ClassAssertions assertions = forObject(repo)

        assertJdbcTemplate assertions

        assertions.with { ac ->
            ac.assertMethod(int, 'countItems', int, int)
        }

        repo.jdbcTemplate = database.jdbcTemplate

        assert repo.countItems(21, 45) == 3
        assert repo.countItems(50, 100) == 1
        assert repo.countItems(0, 10) == 0
    }

    @Test void 'Set<String> listNames()'() {
        def repo = repoBuilder.inject('''
            @SqlSelect('select distinct(name) from someone')
            abstract Set<String> listNames()
        ''').instantiate()

        ClassAssertions assertions = forObject(repo)
        assertJdbcTemplate assertions

        assertions.with { ac ->
            ac.assertMethod(Set, 'listNames')
        }

        repo.jdbcTemplate = database.jdbcTemplate

        def names = repo.listNames()
        assert names.size() == 5
    }

    private static void assertJdbcTemplate(ClassAssertions assertions) {
        assertions.with { repoClass ->
            repoClass.assertField(JdbcTemplate, 'jdbcTemplate').annotatedWith(Autowired)
            repoClass.assertMethod('setJdbcTemplate', JdbcTemplate)
            repoClass.assertMethod(JdbcTemplate, 'getJdbcTemplate')
        }
    }
}
