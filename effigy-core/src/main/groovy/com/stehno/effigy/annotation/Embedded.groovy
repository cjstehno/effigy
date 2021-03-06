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
 * Annotation used to denote an embedded component field. The fields of the embedded object are part of the enclosing table.
 *
 * The type of the embedded object may be an Effigy Entity; however, it need not be. Also, if it is annotated with <code>@Entity</code>, any
 * <code>@Id</code> or <code>@Version</code> annotations will not be honored, since the table data is contained in the table for the enclosing entity.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface Embedded {

    /**
     * The database column name prefix used by the embedded properties. If not specified, the default is to use the name of the entity property.
     */
    String prefix() default ''
}