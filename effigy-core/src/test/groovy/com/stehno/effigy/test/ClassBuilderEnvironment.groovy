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

import org.junit.rules.ExternalResource

import static com.stehno.effigy.test.ClassBuilder.forCode

/**
 * Created by cjstehno on 3/9/15.
 */
class ClassBuilderEnvironment extends ExternalResource {

    private final ClassBuilder builder

    ClassBuilderEnvironment(String source) {
        builder = forCode(source)
    }

    ClassBuilder getBuilder() {
        return builder
    }

    ClassBuilder inject(String code) {
        return builder.inject(code)
    }

    def instance() {
        return builder.instantiate()
    }

    @Override
    protected void after() throws Throwable {
        builder.reset()
    }
}
