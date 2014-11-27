package com.stehno.effigy.annotation

import com.stehno.effigy.transform.EffigyRepositoryTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by cjstehno on 11/26/2014.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@GroovyASTTransformationClass(classes=[EffigyRepositoryTransformer])
@interface EffigyRepository {

    /**
     * The entity type handled by the repository (must be annotated with @Effigy)
     */
    Class forEntity()
}