package com.stehno.effigy.annotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation used to denote that the annotated parameter is to be used as the limit for the query. The annotated
 * parameter should be an Integer or int.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface Limit {
}
