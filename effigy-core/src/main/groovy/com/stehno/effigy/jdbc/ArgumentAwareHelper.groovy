package com.stehno.effigy.jdbc

/**
 * Marks a Sql-annotation helper as being able to accept the method arguments. This method is not required, since
 * simply having the "setMethodArguments()" method available on the helper instance will also work.
 */
interface ArgumentAwareHelper {

    void setMethodArguments(Map<String, Object> args)
}
