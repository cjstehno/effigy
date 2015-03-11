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

import com.stehno.effigy.transform.RepositoryTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*

/**
 * Annotation used to denote a repository managed by the Effigy API.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [RepositoryTransformer])
@interface Repository {

    /**
     * The entity type handled by the repository (must be annotated with @Effigy). This property is required
     * if the CRUD operation annotations are to be used, but may be omitted for the raw SQL annotations.
     */
    Class value() default Void

    /**
     * Used to specify whether or not the JdbcTemplate is to be autowired, the default is 'true'.
     */
    boolean autowired() default true

    /**
     * Used to specify the name of the autowired JdbcTemplate bean, defaults to an empty string, which means the @Qualifier annotation will
     * not be added. This property is ignored if the value of "autowired" is false.
     */
    String qualifier() default ''
}