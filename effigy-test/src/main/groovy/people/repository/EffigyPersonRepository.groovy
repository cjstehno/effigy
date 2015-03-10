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

package people.repository

import com.stehno.effigy.annotation.*
import groovy.transform.Immutable
import people.entity.Person

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Effigy-based implementation of the PersonRepository interface.
 */
@Repository(Person)
abstract class EffigyPersonRepository implements PersonRepository {

    @Create
    abstract Long create(Person person)

    @Retrieve
    abstract List<Person> retrieveAll()

    @Retrieve
    abstract Person retrieve(Long id)

    @Update
    abstract boolean update(Person person)

    @Delete
    abstract boolean delete(Long id)

    @Delete
    abstract boolean deleteAll()

    @Count
    abstract int count(Long id)

    @Count
    abstract int countAll()

    @Exists
    abstract boolean exists(Long id)

    @Retrieve(limit = 2, order = '#id desc')
    abstract List<Person> findTwo()

    @Retrieve(order = '@lastName asc, @firstName asc')
    abstract List<Person> findPaged(@Offset int offset, @Limit int limit)

    @Retrieve
    abstract List<Person> findByLastName(String lastName)

    @SqlSelect('select distinct(last_name) from people where married = :married')
    abstract Set<String> findLastNames(boolean married)

    @SqlSelect('select first_name,middle_name,last_name from people order by last_name,first_name,middle_name')
    @RowMapper(type = NameMapper)
    abstract List<Name> listNames()

    @SqlSelect('select first_name,middle_name,last_name from people where last_name = :lastName order by last_name,first_name,middle_name')
    @RowMapper(type = NameMapper, factory = 'createMapper')
    abstract List<Name> findNames(String lastName)
}

@Immutable
class Name {

    String firstName
    String middleName
    String lastName

    String getFullName() {
        "$firstName $middleName $lastName"
    }
}

class NameMapper implements org.springframework.jdbc.core.RowMapper<Name> {

    @Override
    Name mapRow(ResultSet rs, int rowNum) throws SQLException {
        new Name(rs.getString('first_name'), rs.getString('middle_name'), rs.getString('last_name'))
    }

    static NameMapper createMapper() {
        new NameMapper()
    }
}