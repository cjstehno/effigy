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

package com.stehno.effigy.annotation

import java.lang.annotation.*

/**
 * Annotation used to provide additional information related to the database column represented by the annotated property.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface Column {

    /**
     * The name of the column in the database. If this annotation is not used, the underscored version of the property name will be used as the
     * column name in the database.
     */
    String value()

    /**
     * Used to specify the SQL type of the column (java.sql.Types value). If not set, the default simple type mappings will be used.
     */
    int type() default -2147483648

    // Integer.MIN_VALUE

    /**
     * FIXME: document
     */
    Class handler() default Void
}

// FIXME: move this out
interface FieldTypeHandler<T> {

    T readField()

    void writeField(T)
}
