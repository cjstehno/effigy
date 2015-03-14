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

/**
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

    private static Level level

    static {
        level = Level.valueOf(System.getProperty('effigy.logging', 'INFO').toUpperCase())
    }

    static void info(Class clazz, String msg, Object... args) {
        log Level.INFO, clazz, msg, args
    }

    static void info(Class clazz, String msg, Closure closure) {
        logClos Level.INFO, clazz, msg, closure
    }

    static void trace(Class clazz, String msg, Object... args) {
        log Level.TRACE, clazz, msg, args
    }

    static void trace(Class clazz, String msg, Closure closure) {
        logClos Level.TRACE, clazz, msg, closure
    }

    static void debug(Class clazz, String msg, Object... args) {
        log Level.DEBUG, clazz, msg, args
    }

    static void debug(Class clazz, String msg, Closure closure) {
        logClos Level.DEBUG, clazz, msg, closure
    }

    static void warn(Class clazz, String msg, Object... args) {
        log Level.WARN, clazz, msg, args
    }

    static void warn(Class clazz, String msg, Closure closure) {
        logClos Level.WARN, clazz, msg, closure
    }

    static void error(Class clazz, String msg, Object... args) {
        log Level.ERROR, clazz, msg, args
    }

    static void error(Class clazz, String msg, Closure closure) {
        logClos Level.ERROR, clazz, msg, closure
    }

    private static void log(Level lvl, Class clazz, String msg, Object... args) {
        logClos(lvl, clazz, msg) { args }
    }

    @SuppressWarnings(['Println', 'ParameterReassignment'])
    private static void logClos(Level lvl, Class clazz, String msg, Closure closure) {
        if (level.ordinal() >= lvl.ordinal()) {
            closure().each { arg ->
                msg = msg.replaceFirst(REPLACEMENT_PATTERN, (arg != null ? arg : '<null>') as String)
            }
            println "[${lvl.name()}:${clazz.simpleName}] $msg"
        }
    }
}
