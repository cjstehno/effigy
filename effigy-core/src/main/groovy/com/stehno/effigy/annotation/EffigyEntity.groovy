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

import com.stehno.effigy.transform.EffigyEntityTransformer
import com.stehno.effigy.transform.EffigyResultSetExtractorTransformer
import com.stehno.effigy.transform.EffigyRowMapperTransformer
import com.stehno.effigy.transform.OneToOneAssociationRowMapperTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*

/**
 * Annotation used to denote an Effigy entity object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [
    EffigyEntityTransformer,
    EffigyResultSetExtractorTransformer,
    OneToOneAssociationRowMapperTransformer,
    EffigyRowMapperTransformer
])
@interface EffigyEntity {

    /**
     * The name of the database table represented by the entity. The default will be to use the pluralized name of the entity.
     */
    String table() default ''
}