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

import com.stehno.effigy.transform.FindOperationsTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*

/**
 * Annotation used to inject Finder operations into a repository. The repository class must also be annotated with the Effigy @Repository annotation.
 *
 * This annotation injects the CrudOperations.count(entityId), CrudOperations.count() CrudOperations.exists(entityId) methods.
 *
 * With the @FindOperations annotation you also et added finder methods based on methods defined by the interface or abstract methods of the annotated
 * repository. All methods of the name pattern "findBy[propName][[And][propName]...]" will be used to generate finder methods based on the method
 * name. Concrete methods with names matching the pattern will be ignored.
 *
 * The return type of the method should be the entity type in collection or array form, which will be used in generating the method
 * implementation.
 *
 * Finders will not throw an exception if there is no data is retrieved.
 *
 * Finder query criteria based on properties is only allowed for properties of the entity itself, not associations or embedded properties.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes = [FindOperationsTransformer])
@interface FindOperations {

}