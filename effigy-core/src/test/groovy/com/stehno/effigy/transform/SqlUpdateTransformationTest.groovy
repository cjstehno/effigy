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

import com.stehno.effigy.jdbc.EffigyPreparedStatementSetter
import com.stehno.effigy.test.ClassAssertions
import com.stehno.effigy.test.ClassBuilderEnvironment
import com.stehno.effigy.test.DatabaseEnvironment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.support.StaticApplicationContext
import org.springframework.jdbc.core.JdbcTemplate

import java.sql.PreparedStatement

import static com.stehno.effigy.test.ClassAssertions.forObject

class SqlUpdateTransformationTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment(schema: 'schema-a.sql', data: 'data-a.sql')

    @Rule public ClassBuilderEnvironment classBuilder = new ClassBuilderEnvironment('''
        package testing

        import com.stehno.effigy.annotation.*

        @Repository
        abstract class UpdateTestRepository {
            $code
        }
    ''')

    private StaticApplicationContext applicationContext

    @Before void before() {
        applicationContext = new StaticApplicationContext()
    }

    @Test void 'int addSomeone(String name, int age)'() {
        def repo = repository('''
            @SqlUpdate('insert into someone (name,age) values (:name,:age)')
            abstract int addSomeone(String name, int age)

            @SqlSelect('select count(*) from someone')
            abstract int count()
        ''') { ac ->
            ac.assertMethod(int, 'count')
            ac.assertMethod(int, 'addSomeone', String, int)
        }

        assert repo.count() == 6

        assert repo.addSomeone('Jose', 53) == 1

        assert repo.count() == 7
    }

    @Test void 'long addSomeone(String name, int age):setter'() {
        def repo = repository('''
            @SqlUpdate('insert into someone (name,age) values (?,?)')
            @PreparedStatementSetter(type=com.stehno.effigy.transform.SomeoneSetter, arguments=true)
            abstract long addSomeone(String name, int age)

            @SqlSelect('select count(*) from someone')
            abstract int count()
        ''') { ac ->
            ac.assertMethod(int, 'count')
            ac.assertMethod(long, 'addSomeone', String, int)
        }

        assert repo.count() == 6

        assert repo.addSomeone('Jose', 53) == 1

        assert repo.count() == 7
    }

    @Test void 'boolean updateAge(String name, int age)'() {
        def repo = repository('''
            @SqlUpdate('update someone set age=:age where name=:name')
            abstract boolean updateAge(String name, int age)

            @SqlSelect('select age from someone order by age')
            abstract List<Integer> ages()
        ''') { ac ->
            ac.assertMethod(List, 'ages')
            ac.assertMethod(boolean, 'updateAge', String, int)
        }

        assert repo.ages().containsAll([18, 20, 29, 36, 42, 56])

        assert repo.updateAge('Curley', 21)

        assert repo.ages().containsAll([18, 21, 29, 36, 42, 56])
    }

    // FIXME: this needs to be pulled out to a shared location
    private repository(String code, Closure asserts) {
        def repo = classBuilder.inject(code).instantiate()

        ClassAssertions assertions = forObject(repo)
        assertJdbcTemplate assertions

        assertions.with asserts

        repo.jdbcTemplate = database.jdbcTemplate
        repo
    }

    private static void assertJdbcTemplate(ClassAssertions assertions) {
        assertions.with { repoClass ->
            repoClass.assertField(JdbcTemplate, 'jdbcTemplate').annotatedWith(Autowired)
            repoClass.assertMethod('setJdbcTemplate', JdbcTemplate)
            repoClass.assertMethod(JdbcTemplate, 'getJdbcTemplate')
        }
    }
}

class SomeoneSetter extends EffigyPreparedStatementSetter {

    @SuppressWarnings('GroovyAssignabilityCheck')
    @Override void setValues(PreparedStatement ps, Map<String, Object> arguments) {
        ps.setString(1, arguments.name)
        ps.setInt(2, arguments.age)
    }
}