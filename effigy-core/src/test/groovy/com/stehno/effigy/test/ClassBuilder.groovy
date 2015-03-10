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

import groovy.text.GStringTemplateEngine
import groovy.text.Template

/**
 * Template-based source code compiler used to build and test AST transformation code at runtime.
 */
class ClassBuilder {

    private final Template template
    private final injectedBlocks = []

    private ClassBuilder(String classBase) {
        template = new GStringTemplateEngine().createTemplate(classBase)
    }

    static ClassBuilder forCode(String classBase) {
        new ClassBuilder(classBase)
    }

    ClassBuilder inject(String code) {
        injectedBlocks << code
        this
    }

    ClassBuilder reset() {
        injectedBlocks.clear()
        this
    }

    String source() {
        template.make(code: injectedBlocks.join('\n'))
    }

    Class compile() {
        new GroovyClassLoader().parseClass(source())
    }

    def instantiate() {
        compile().newInstance()
    }
}
