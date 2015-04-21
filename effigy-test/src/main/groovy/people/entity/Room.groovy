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

package people.entity

import com.stehno.effigy.annotation.Association
import com.stehno.effigy.annotation.Entity
import com.stehno.effigy.annotation.Id
import com.stehno.effigy.annotation.Mapped
import groovy.transform.Canonical

/**
 * Created by cjstehno on 12/26/14.
 */
@Entity @Canonical
class Room {

    @Id long id
    String name
    int capacity

    @Association @Mapped(keyProperty = 'type')
    Map<Feature.Type, Feature> features = [:]
}
