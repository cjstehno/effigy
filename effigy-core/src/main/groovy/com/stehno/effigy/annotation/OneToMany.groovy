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

import java.lang.annotation.*
/**
 * ... association of one entity to many associated entities..
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface OneToMany {

    /**
     * Name of the association table. If not explicitly specified, the table name will be the name of the
     * entity table and the name of the entity association property separated by an underscore.
     */
    String table() default ''

    /**
     * The id column name of the owning entity. If not explicitly specified, the entity id field name will
     * be the entity table name with the suffix '_id' appended to it.
     */
    String entityId() default ''

    /**
     * The id column name of the associated entity. If not explicitly specified, the association id field name will
     * be the associated entity table name with the suffix '_id' appended to it.
     */
    String associationId() default ''
}
