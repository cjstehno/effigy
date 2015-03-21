/*
 * Copyright (c) 2015 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stehno.effigy.logging

import groovy.transform.Memoized

/**
 *  FIXME: update docs and add to guide  - why not prexisting logger api? classloader issues
 *
 * Static logging mechanism for logging during AST transformation activity.
 *
 * The default logging level is WARN, which may be changed by setting the "effigy.logging" system property to a value of TRACE, DEBUG, INFO, WARN,
 * ERROR, ALL, or OFF.
 *
 * The log messages are written to the console during compilation - note that this logging will only occur during the compilation such that if no
 * compilation occurs, not logging will be written (e.g. in the case where class files already exist and the source has not been modified).
 */
class Logger {

    private static final String REPLACEMENT_PATTERN = /\{\}/

    static enum Level {
        OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    }

    // FIXME: how to configure
    private final Level level = Level.valueOf(System.getProperty('effigy.logging.level', 'INFO').toUpperCase())
    private final Class loggedClass

    private Logger(Class loggedClass) {
        this.loggedClass = loggedClass
    }

    @Memoized
    static Logger factory(Class loggedClass) {
        new Logger(loggedClass)
    }

    void info(String msg, Object... args) {
        log Level.INFO, loggedClass, msg, args
    }

    void info(String msg, Closure closure) {
        logClos Level.INFO, loggedClass, msg, closure
    }

    void trace(String msg, Object... args) {
        log Level.TRACE, loggedClass, msg, args
    }

    void trace(String msg, Closure closure) {
        logClos Level.TRACE, loggedClass, msg, closure
    }

    void debug(String msg, Object... args) {
        log Level.DEBUG, loggedClass, msg, args
    }

    void debug(String msg, Closure closure) {
        logClos Level.DEBUG, loggedClass, msg, closure
    }

    void warn(String msg, Object... args) {
        log Level.WARN, loggedClass, msg, args
    }

    void warn(String msg, Closure closure) {
        logClos Level.WARN, loggedClass, msg, closure
    }

    void error(String msg, Object... args) {
        log Level.ERROR, loggedClass, msg, args
    }

    void error(String msg, Closure closure) {
        logClos Level.ERROR, loggedClass, msg, closure
    }

    private void log(Level lvl, Class clazz, String msg, Object... args) {
        logClos(lvl, clazz, msg) { args }
    }

    @SuppressWarnings(['Println', 'ParameterReassignment'])
    private void logClos(Level lvl, Class clazz, String msg, Closure closure) {
        if (level.ordinal() >= lvl.ordinal()) {
            closure().each { arg ->
                msg = msg.replaceFirst(REPLACEMENT_PATTERN, (arg != null ? arg : '<null>') as String)
            }
            println "[${lvl.name()}:${clazz.simpleName}] $msg"
        }
    }
}