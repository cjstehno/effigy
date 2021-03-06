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

package people.entity

import com.stehno.effigy.annotation.Column
import com.stehno.effigy.annotation.Entity
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.annotation.Version
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import static java.sql.Types.VARCHAR

@Entity @EqualsAndHashCode @ToString(includeNames = true)
class Image {

    @Id Long id
    @Version Long version

    String description

    @Column(value = 'cont_len', type = VARCHAR, handler = ContentLengthHandler)
    long contentLength
}

class ContentLengthHandler {

    static long readField(String field) {
        (field - ' bytes') as long
    }

    static String writeField(long value) {
        "$value bytes"
    }
}
