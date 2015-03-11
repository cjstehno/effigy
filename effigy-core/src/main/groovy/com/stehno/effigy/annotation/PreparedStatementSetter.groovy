package com.stehno.effigy.annotation

import java.lang.annotation.*

/**
 * This annotation is used with a SqlSelect or SqlUpdate annotation to provide information about the PreparedStatementSetter instance to be used.
 *
 * There are three distinct configuration scenarios for setter annotations, based on the annotation property values:
 *
 * * the 'bean' property can be used to specify the name of a bean (implementing PreparedStatementSetter) which will be autowired into the
 *   repository and used by the query.
 * * the 'type' property can be used to specify a class implementing PreparedStatementSetter which will be instantiated as a shared instance
 *   and used by the query.
 * * the 'type' and 'factory' properties may be used similar to the 'type' property alone, except that the specified static factory method
 *   will be called to create the instance, rather than the constructor.
 *
 * Any implementation of PreparedStatementSetter may be used; however, if an extension of the EffigyPreparedStatementSetter is used, the instance
 * will have access to the arguments of the decorated method at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface PreparedStatementSetter {

    // TODO: annotations can have interfaces, might be nice to have the helpers all implement a common interface
    // TODO: this needs to be documented in the user guide

    /**
     * The name of the bean to be autowired into the repository for use as a PreparedStatementSetter instance. If the default value is used,
     * this property will be ignored.
     */
    String bean() default ''

    /**
     * The class implementing PreparedStatementSetter that is to be used by the query. If the default value is used, this property will be ignored.
     */
    Class type() default Void.class

    /**
     * The name of a static factory method on the class provided by the 'type' property. This property is ignored unless a 'type' value is specified.
     */
    String factory() default ''

    // FIXME: document here and user guide
    boolean singleton() default true
}