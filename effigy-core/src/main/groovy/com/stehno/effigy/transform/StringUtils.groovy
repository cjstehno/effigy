package com.stehno.effigy.transform

/**
 * Created by cjstehno on 11/26/2014.
 */
class StringUtils {

    static String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }

    static String camelCaseToUnderscore(String camel) {
        camel.replaceAll(/\B[A-Z]/) { '_' + it }.toLowerCase()
    }
}
