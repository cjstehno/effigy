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
import com.stehno.effigy.test.ClassBuilderEnvironment
import com.stehno.effigy.test.DatabaseEnvironment
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet
import java.sql.SQLException

import static com.stehno.effigy.test.ClassAssertions.forObject

class SqlSelectTransformationTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment(schema: 'schema-a.sql', data: 'data-a.sql')

    @Rule public ClassBuilderEnvironment classBuilder = new ClassBuilderEnvironment('''
        package testing

        import com.stehno.effigy.annotation.*

        @Repository
        abstract class SelectorTestRepository {
            $code
        }
    ''')

    @Test void 'int countItems(int min, int max)'() {
        classBuilder.inject('''
            @SqlSelect('select count(*) from someone where age > :min and age < :max')
            abstract int countItems(int min, int max)
        ''')

        def repo = classBuilder.instance()

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
        def repo = classBuilder.inject('''
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

    @Test void 'List<String> listAll():type'() {
        def repo = classBuilder.inject('''
            @SqlSelect('select name,age from someone order by age')
            @RowMapper(type='com.stehno.effigy.transform.SomeoneRowMapper')
            abstract List<String> listAll()
        ''').instantiate()

        ClassAssertions assertions = forObject(repo)
        assertJdbcTemplate assertions

        assertions.with { ac ->
            ac.assertMethod(List, 'listAll')
        }

        repo.jdbcTemplate = database.jdbcTemplate

        def items = repo.listAll()
        assert items.size() == 6
        assert items[0] == [name: 'Bob', age: 18]
        assert items[1] == [name: 'Curley', age: 20]
        assert items[2] == [name: 'Larry', age: 29]
        assert items[3] == [name: 'Chris', age: 36]
        assert items[4] == [name: 'Chris', age: 42]
        assert items[5] == [name: 'Moe', age: 56]
    }

    @Test void 'List<String> listAll():type+factory'() {
        def repo = classBuilder.inject('''
            @SqlSelect('select name,age from someone order by age')
            @RowMapper(type='com.stehno.effigy.transform.SomeoneRowMapper', factory='mapper')
            abstract List<String> listAll()
        ''').instantiate()

        ClassAssertions assertions = forObject(repo)
        assertJdbcTemplate assertions

        assertions.with { ac ->
            ac.assertMethod(List, 'listAll')
        }

        repo.jdbcTemplate = database.jdbcTemplate

        def items = repo.listAll()
        assert items.size() == 6
        assert items[0] == [name: 'Bob', age: 18]
        assert items[1] == [name: 'Curley', age: 20]
        assert items[2] == [name: 'Larry', age: 29]
        assert items[3] == [name: 'Chris', age: 36]
        assert items[4] == [name: 'Chris', age: 42]
        assert items[5] == [name: 'Moe', age: 56]
    }

    private static void assertJdbcTemplate(ClassAssertions assertions) {
        assertions.with { repoClass ->
            repoClass.assertField(JdbcTemplate, 'jdbcTemplate').annotatedWith(Autowired)
            repoClass.assertMethod('setJdbcTemplate', JdbcTemplate)
            repoClass.assertMethod(JdbcTemplate, 'getJdbcTemplate')
        }
    }
}

class SomeoneRowMapper implements RowMapper<Map<String, Object>> {

    static mapper() {
        new SomeoneRowMapper()
    }

    @Override
    Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        [
            name: rs.getString(1),
            age : rs.getInt(2)
        ]
    }
}
