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

package com.stehno.effigy.test

import java.lang.reflect.Field

/**
 * Created by cjstehno on 2/28/15.
 */
class ClassAssertions {

    private final Object object

    private ClassAssertions(final Object object) {
        this.object = object
    }

    static ClassAssertions forObject(Object obj) {
        new ClassAssertions(obj)
    }

    void assertMethod(Class returnType = null, String name, Object... args) {
        MetaMethod method = object.metaClass.getMetaMethod(name, args)
        assert method
        if (returnType) {
            assert method.returnType == returnType
        }
    }

    FieldAssertions assertField(Class type, String name) {
        Field field = object.class.getDeclaredField(name)
        assert field
        assert field.type == type

        FieldAssertions.forField(field)
    }
}

class FieldAssertions {

    private final Field field

    private FieldAssertions(final Field field) {
        this.field = field
    }

    static FieldAssertions forField(Field f) {
        new FieldAssertions(f)
    }

    void annotatedWith(Class annotClass) {
        assert field.getAnnotation(annotClass)
    }
}