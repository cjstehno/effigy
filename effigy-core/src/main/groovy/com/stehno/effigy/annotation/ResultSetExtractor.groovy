package com.stehno.effigy.annotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * This annotation is used with a SqlSelect annotation to provide information about the ResultSetExtractor instance to be used.
 *
 * There are three distinct configuration scenarios for extractor annotations, based on the annotation property values:
 *
 * * the 'bean' property can be used to specify the name of a bean (implementing ResultSetExtractor) which will be autowired into the
 *   repository and used by the query.
 * * the 'type' property can be used to specify a class implementing ResultSetExtractor which will be instantiated as a shared instance
 *   and used by the query.
 * * the 'type' and 'factory' properties may be used similar to the 'type' property alone, except that the specified static factory method
 *   will be called to create the instance, rather than the constructor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface ResultSetExtractor {

    /**
     * The name of the bean to be autowired into the repository for use as a ResultSetExtractor instance. If the default value is used,
     * this property will be ignored.
     */
    String bean() default ''

    /**
     * The class implementing ResultSetExtractor that is to be used by the query. If the default value is used, this property will be ignored.
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

    // FIXME: document in user guide
    /**
     * Whether or not the created instance of this helper class should have the method arguments provided to it. The default is false.
     *
     * If the arguments are to be provided, the value of the 'singleton' property should be 'false' since the helper will no longer be stateless.
     * The implementation of the PreparedStatementSetter must either implement the ArgumentAwareHelper class or provide a method with the
     * following signature to accept the method parameters.
     *
     * public void setMethodArguments(Map<String,Object> map)
     *
     * In the provided method case, it is up to the implementation to store and use the arguments properly.
     */
    boolean arguments() default false
}