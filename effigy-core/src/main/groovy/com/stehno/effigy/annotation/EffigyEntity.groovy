package com.stehno.effigy.annotation

import com.stehno.effigy.transform.EffigyEntityTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.*
/**
 * Created by cjstehno on 11/26/2014.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes=[EffigyEntityTransformer])
@interface EffigyEntity {

    /**
     * The name of the database table represented by the entity. The default will be to use the pluralized name of the entity.
     */
    String table() default ''
}