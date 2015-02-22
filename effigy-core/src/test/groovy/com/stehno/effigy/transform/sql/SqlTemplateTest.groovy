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

package com.stehno.effigy.transform.sql

import org.junit.Test

class SqlTemplateTest {

    @Test void 'multiple replacements'() {
        def template = new SqlTemplate('select @a,#b,@c from somewhere where x = :x and y = :x')

        assert template.propertyNames().size() == 2
        assert template.propertyNames().containsAll(['@a', '@c'])

        assert template.macroNames().size() == 1
        assert template.macroNames().contains('#b')

        // TODO: not sure this is what I want to happen or not, investigate further
        assert template.variableNames().size() == 2
    }
}
