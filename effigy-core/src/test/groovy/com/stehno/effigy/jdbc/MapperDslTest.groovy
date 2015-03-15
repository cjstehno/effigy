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

package com.stehno.effigy.jdbc

import groovy.transform.ToString
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.jdbc.core.RowMapper

import java.sql.ResultSet

import static com.stehno.effigy.jdbc.MapperDsl.mapper
import static org.mockito.Mockito.when

@RunWith(MockitoJUnitRunner)
class MapperDslTest {

    @Mock private ResultSet resultSet

    @Test void mapping() {
        RowMapper<InterestingObject> rowMapper = mapper(InterestingObject) {
            map 'part_name' into 'partName'
            map 'some_date' using { x -> new Date(x) } into 'someDate'
            mapProperty 'number'
            //            map 'sub_id' into 'sub.id'
            //            map 'sub_label' into 'sub.label'
        }

        assert rowMapper

        when(resultSet.getObject('part_name')).thenReturn('OXY937')
        when(resultSet.getObject('some_date')).thenReturn(System.currentTimeMillis())
        when(resultSet.getObject('number')).thenReturn(9876)
        when(resultSet.getObject('sub_id')).thenReturn(2467L)
        when(resultSet.getObject('sub_label')).thenReturn('Other')

        def output = rowMapper.mapRow(resultSet, 0)
        println output
    }
}

@ToString
class InterestingObject {

    String partName
    Date someDate
    int number
    SubObject sub
}

@ToString
class SubObject {
    long id
    String label
}