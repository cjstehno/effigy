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

package com.stehno.effigy.annotation
import com.stehno.effigy.transform.EffigyRepositoryTransformer
import com.stehno.effigy.transform.EffigyResultSetExtractorInjector
import com.stehno.effigy.transform.EffigyRowMapperInjector
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*
/**
 * Annotation used to denote a repository managed by the Effigy API.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [
    EffigyRepositoryTransformer,
    EffigyResultSetExtractorInjector,
    EffigyRowMapperInjector
])
@interface EffigyRepository {

    /**
     * The entity type handled by the repository (must be annotated with @Effigy)
     */
    Class forEntity()
}