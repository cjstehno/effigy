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
 * This annotation is used with a SqlSelect or SqlUpdate annotation to provide information about the PreparedStatementSetter instance to be used.
 *
 * There are three distinct configuration scenarios for setter annotations, based on the annotation property values:
 * <ul>
 *     <li>the 'bean' property can be used to specify the name of a bean (implementing PreparedStatementSetter) which will be autowired into the
 *     repository and used by the query.</li>
 *     <li>the 'type' property can be used to specify a class implementing <code>PreparedStatementSetter</code> which will be instantiated as a
 *     shared instance and used by the query.</li>
 *     <li>the 'type' and 'factory' properties may be used similar to the 'type' property alone, except that the specified static factory method
 *     will be called to create the instance, rather than the constructor.</li>
 * </ul>
 *
 * Any implementation of <code>PreparedStatementSetter</code> may be used; however, if an extension of the <code>EffigyPreparedStatementSetter</code>
 * is used, the instance will have access to the arguments of the decorated method at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface PreparedStatementSetter {

    /**
     * The name of the bean to be autowired into the repository for use as a <code>PreparedStatementSetter</code> instance. If the default value is
     * used, this property will be ignored.
     */
    String bean() default ''

    /**
     * The class implementing <code>PreparedStatementSetter</code> that is to be used by the query. If the default value is used, this property will
     * be ignored.
     */
    Class type() default Void.class

    /**
     * The name of a static factory method on the class provided by the 'type' property. This property is ignored unless a 'type' value is specified.
     */
    String factory() default ''

    /**
     * Whether or not the created instance of this helper class should be shared. The default is true.
     */
    boolean singleton() default true

    /**
     * Whether or not the created instance of this helper class should have the method arguments provided to it. The default is false.
     *
     * If the arguments are to be provided, the value of the 'singleton' property should be 'false' since the helper will no longer be stateless.
     * The implementation of the <code>PreparedStatementSetter</code> must either implement the <code>ArgumentAwareHelper</code> class or provide a
     * method with the following signature to accept the method parameters.
     *
     * <pre>public void setMethodArguments(Map<String,Object> map)</pre>
     *
     * In the provided method case, it is up to the implementation to store and use the arguments properly.
     */
    boolean arguments() default false
}