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

import com.stehno.effigy.transform.CreateOperationsTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*

/**
 * Annotation used to inject Create operations into a repository. The repository class must also be annotated with the @EffigyRepository annotation.
 *
 * This annotation injects the CrudOperations.create(entity) method.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [CreateOperationsTransformer])
@interface CreateOperations {

}