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
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticApplicationContext
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper

import java.sql.PreparedStatement
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

    private StaticApplicationContext applicationContext

    @Before void before(){
        applicationContext = new StaticApplicationContext()
    }

    @Test void 'int countItems(int min, int max)'() {
        def repo = repository('''
            @SqlSelect('select count(*) from someone where age > :min and age < :max')
            abstract int countItems(int min, int max)
        '''){ ac ->
            ac.assertMethod(int, 'countItems', int, int)
        }

        assert repo.countItems(21, 45) == 3
        assert repo.countItems(50, 100) == 1
        assert repo.countItems(0, 10) == 0
    }

    @Test void 'Set<String> listNames()'() {
        def repo = repository('''
            @SqlSelect('select distinct(name) from someone')
            abstract Set<String> listNames()
        '''){ ac ->
            ac.assertMethod(Set, 'listNames')
        }

        assert repo.listNames().size() == 5
    }

    @Test void 'List<String> listAll():type'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone order by age')
            @RowMapper(type=com.stehno.effigy.transform.SomeoneRowMapper)
            abstract List<String> listAll()
        '''){ ac ->
            ac.assertMethod(List, 'listAll')
            ac.assertField(RowMapper, '_SomeoneRowMapper')
        }

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
        def repo = repository('''
            @SqlSelect('select name,age from someone order by age')
            @RowMapper(type=com.stehno.effigy.transform.SomeoneRowMapper, factory='mapper')
            abstract List<String> listAll()
        '''){ ac ->
            ac.assertMethod(List, 'listAll')
            ac.assertField(RowMapper, '_SomeoneRowMapper_Mapper')
        }

        def items = repo.listAll()
        assert items.size() == 6
        assert items[0] == [name: 'Bob', age: 18]
        assert items[1] == [name: 'Curley', age: 20]
        assert items[2] == [name: 'Larry', age: 29]
        assert items[3] == [name: 'Chris', age: 36]
        assert items[4] == [name: 'Chris', age: 42]
        assert items[5] == [name: 'Moe', age: 56]
    }

    @Test void 'List<String> listAll():bean'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone order by age')
            @RowMapper(bean='someoneMapper')
            abstract List<String> listAll()
        '''){ ac ->
            ac.assertMethod(List, 'listAll')
            ac.assertField(RowMapper, 'someoneMapper')
        }

        repo.someoneMapper = new SomeoneRowMapper()

        def items = repo.listAll()
        assert items.size() == 6
        assert items[0] == [name: 'Bob', age: 18]
        assert items[1] == [name: 'Curley', age: 20]
        assert items[2] == [name: 'Larry', age: 29]
        assert items[3] == [name: 'Chris', age: 36]
        assert items[4] == [name: 'Chris', age: 42]
        assert items[5] == [name: 'Moe', age: 56]
    }

    @Test void 'String grouping(int min, int max):type'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @ResultSetExtractor(type=com.stehno.effigy.transform.SomeoneResultSetExtractor)
            abstract String grouping(int min, int max)
        '''){ ac ->
            ac.assertMethod(String, 'grouping', int, int)
            ac.assertField(ResultSetExtractor, '_SomeoneResultSetExtractor')
        }

        assert repo.grouping(15,30) == 'Bob (18) & Curley (20) & Larry (29)'
    }

    @Test void 'String groupings(int min, int max):type+factory'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @ResultSetExtractor(type=com.stehno.effigy.transform.SomeoneResultSetExtractor, factory='extractor')
            abstract String grouping(int min, int max)
        '''){ ac ->
            ac.assertMethod(String, 'grouping', int, int)
            ac.assertField(ResultSetExtractor, '_SomeoneResultSetExtractor_Extractor')
        }

        assert repo.grouping(15,30) == 'Bob (18) & Curley (20) & Larry (29)'
    }

    @Test void 'String groupings(int min, int max):bean'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @ResultSetExtractor(bean='someoneExtractor')
            abstract String grouping(int min, int max)
        '''){ ac ->
            ac.assertMethod(String, 'grouping', int, int)
            ac.assertField(ResultSetExtractor, 'someoneExtractor')
        }

        repo.someoneExtractor = new SomeoneResultSetExtractor()

        assert repo.grouping(15,30) == 'Bob (18) & Curley (20) & Larry (29)'
    }

    @Test void 'String grouping():setter:type+factory'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @PreparedStatementSetter(type=com.stehno.effigy.transform.AgeRangeSetter, factory='middleAged')
            @ResultSetExtractor(type=com.stehno.effigy.transform.SomeoneResultSetExtractor)
            abstract String grouping()
        '''){ ac ->
            ac.assertMethod(String, 'grouping')
            ac.assertField(PreparedStatementSetter, '_AgeRangeSetter_MiddleAged')
            ac.assertField(ResultSetExtractor, '_SomeoneResultSetExtractor')
        }

        assert repo.grouping() == 'Chris (42) & Moe (56)'
    }

    @Test void 'String grouping():setter:bean/singleton'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @PreparedStatementSetter(bean='middleAged')
            @ResultSetExtractor(type=com.stehno.effigy.transform.SomeoneResultSetExtractor)
            abstract String grouping()
        '''){ ac ->
            ac.assertMethod(String, 'grouping')
            ac.assertField(PreparedStatementSetter, 'middleAged')
            ac.assertField(ResultSetExtractor, '_SomeoneResultSetExtractor')
        }

        repo.middleAged = AgeRangeSetter.middleAged()

        assert repo.grouping() == 'Chris (42) & Moe (56)'
    }

    @Test void 'String grouping():setter:bean-prototype'() {
        def repo = repository('''
            @SqlSelect('select name,age from someone where age > :min and age < :max order by age')
            @PreparedStatementSetter(bean='middleAged', singleton=false)
            @ResultSetExtractor(type=com.stehno.effigy.transform.SomeoneResultSetExtractor)
            abstract String grouping()
        '''){ ac ->
            ac.assertMethod(String, 'grouping')
            ac.assertField(ApplicationContext, 'applicationContext')
            ac.assertField(ResultSetExtractor, '_SomeoneResultSetExtractor')
        }

        applicationContext.registerPrototype('middleAged', AgeRangeSetter, new MutablePropertyValues([min:40, max: 60]) )

        repo.applicationContext = applicationContext

        assert repo.grouping() == 'Chris (42) & Moe (56)'
    }

    @Test void 'List<String> search(String term):epss-setter'() {
        def repo = repository('''
            @SqlSelect('select name from someone where name like ? order by age')
            @PreparedStatementSetter(type=com.stehno.effigy.transform.NameSearchSetter, arguments=true)
            abstract List<String> search(String term)
        '''){ ac ->
            ac.assertMethod(List, 'search', String)
        }

        repo.jdbcTemplate = database.jdbcTemplate

        assert repo.search('e').size() == 2
    }

    private static void assertJdbcTemplate(ClassAssertions assertions) {
        assertions.with { repoClass ->
            repoClass.assertField(JdbcTemplate, 'jdbcTemplate').annotatedWith(Autowired)
            repoClass.assertMethod('setJdbcTemplate', JdbcTemplate)
            repoClass.assertMethod(JdbcTemplate, 'getJdbcTemplate')
        }
    }

    private repository(String code, Closure asserts){
        def repo = classBuilder.inject(code).instantiate()

        ClassAssertions assertions = forObject(repo)
        assertJdbcTemplate assertions

        assertions.with asserts

        repo.jdbcTemplate = database.jdbcTemplate
        repo
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

class SomeoneResultSetExtractor implements ResultSetExtractor<String> {

    static extractor(){
        new SomeoneResultSetExtractor()
    }

    @Override
    String extractData(ResultSet rs) throws SQLException, DataAccessException {
        def rows = []
        while(rs.next()){
            rows << "${rs.getString(1)} (${rs.getInt(2)})"
        }
        rows.join(' & ')
    }
}

class AgeRangeSetter implements PreparedStatementSetter {

    int min
    int max

    static middleAged(){
        new AgeRangeSetter(min: 40, max: 60)
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setInt(1, min)
        ps.setInt(2, max)
    }
}

class NameSearchSetter extends EffigyPreparedStatementSetter {

    @Override
    void setValues(PreparedStatement ps, Map<String, Object> arguments) {
        ps.setString(1, "%${arguments.term}%")
    }
}