package com.stehno.effigy.annotation

import java.lang.annotation.*

/**
 * Annotation used to denote that the annotated parameter is to be used as the limit for the query. The annotated
 * parameter should be an Integer or int.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Limit {
}
