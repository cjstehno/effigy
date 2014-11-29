package com.stehno.effigy.annotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * ... association of one entity to many associated entities..
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface OneToMany {

    // NOTE: saving does not affect actual child refs, just managed associations (use repo to delete/add)
    /// FIXME: this needs to be documented

    /**
     * Name of the association table...
     *
     * @return
     */
    String table()

    String entityId()
    String associationId()
}
