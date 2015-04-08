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

package com.stehno.effigy.dsl

import groovy.transform.ToString
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet

import static com.stehno.effigy.jdbc.RowMapperBuilder.mapper
import static org.mockito.Mockito.when

@RunWith(MockitoJUnitRunner)
class RowMapperBuilderTest {

    @Mock private ResultSet resultSet

    @Test void mapping() {
        RowMapper<InterestingObject> rowMapper = mapper(InterestingObject) {
            map 'partName'
            map 'someDate' using { x -> new Date(x) }
            map 'items' from 'line_items' using { x -> x.split(';') }
            map 'lineNumber' from 'line_number'
            map 'something' using mapper(EmbedddObject, 'obj_') {
                map 'id'
                map 'label'
            }
        }

        assert rowMapper

        when(resultSet.getObject('part_name')).thenReturn('OXY937')
        when(resultSet.getObject('some_date')).thenReturn(System.currentTimeMillis())
        when(resultSet.getObject('line_number')).thenReturn(9876)
        when(resultSet.getObject('line_items')).thenReturn('alpha;bravo;charlie')
        when(resultSet.getObject('obj_id')).thenReturn(123987)
        when(resultSet.getObject('obj_label')).thenReturn('Foobar')

        def output = rowMapper.mapRow(resultSet, 0)

        assert output
        assert output.partName == 'OXY937'
        assert output.lineNumber == 9876
        assert output.someDate
        assert output.items.containsAll(['alpha', 'bravo', 'charlie'])
        assert output.something.id == 123987
        assert output.something.label == 'Foobar'
    }
}

@ToString
class InterestingObject {

    String partName
    Date someDate
    int lineNumber
    List<String> items
    EmbedddObject something
}

@ToString
class EmbedddObject {
    long id
    String label
}