package com.stehno.effigy.logging

/**
 * Created by cjstehno on 11/29/2014.
 */
class Logger {

    static enum Level { OFF, WARN, INFO, DEBUG, ALL }

    // FIXME: need to wire this into a system property
    static Level level = Level.ALL

    static void info(Class clazz, String msg, Object... args){
        log Level.INFO, clazz, msg, args
    }

    static void info(Class clazz, String msg, Closure closure){
        logClos Level.INFO, clazz, msg, closure
    }

    static void debug(Class clazz, String msg, Object... args){
        log Level.DEBUG,clazz,  msg, args
    }

    static void debug(Class clazz, String msg, Closure closure){
        logClos Level.DEBUG,clazz,  msg, closure
    }

    static void warn(Class clazz, String msg, Object... args){
        log Level.WARN, clazz, msg, args
    }

    static void warn(Class clazz, String msg, Closure closure){
        logClos Level.WARN, clazz, msg, closure
    }

    private static void log(Level lvl, Class clazz, String msg, Object... args) {
        if (level.ordinal() >= lvl.ordinal()) {
            args.each { arg->
                msg = msg.replaceFirst(/\{\}/, arg as String)
            }
            println "[${lvl.name()}:${clazz.simpleName}] $msg"
        }
    }

    private static void logClos(Level lvl, Class clazz, String msg, Closure closure) {
        if (level.ordinal() >= lvl.ordinal()) {
            closure().each { arg->
                msg = msg.replaceFirst(/\{\}/, arg as String)
            }
            println "[${lvl.name()}:${clazz.simpleName}] $msg"
        }
    }
}
