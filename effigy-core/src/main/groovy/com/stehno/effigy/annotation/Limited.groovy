package com.stehno.effigy.annotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation used to specify that a finder should return only a limited number of results. This annotation should be
 * used to annotate a finder method whose limit is to be compiled into the query. If you need the ability to specify
 * the limit at runtime, use the @Limit annotation to annotate the limit parameter instead.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface Limited {

    /**
     * The value used to limit the number of results returned from the query.
     */
    int value()
}