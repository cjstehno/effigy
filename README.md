
# Effigy

> WARNING: this project is not ready for general use yet - in fact, it is effectively dormant. The different SQL dialects and deeper use cases make this project overly difficult to implement (for free). I think this is still a valid project, it's just too much work unless I either need it for something or get paid to develop it. If you would like to donate code or money towards its further development, please feel free to contact me.

## Introduction

> Effigy: 1. a sculpture or model of a person. 2. a roughly made model of a particular person, made in order to be damaged or destroyed as a
protest or expression of anger

You can decide which definition above fits with your standard data-access experience.

Effigy is an annotation-driven simplification of JDBC that sits on top of the Spring JDBC library and allows a developer
to quickly develop data-access code for standard use cases, based on standard coding patterns.

Effigy is NOT a full-blown ORM.

Effigy is inspired by JPA and Spring-Data JPA and the desire to have similar functionality with simple JDBC-based projects.

## Build

Effigy uses Gradle, so just run:

    gradlew build

## User Guide

The User Guide is built using [Gaiden](http://kobo.github.io/gaiden), separate from the standard Gradle build. If you want to generate the User Guide
run the following in the `effigy-guide` directory:

    gaidenw build

The `effigy-guide/build` directory will contain the generated guide.


**Site:** http://cjstehno.github.io/effigy

[![Build Status](https://drone.io/github.com/cjstehno/effigy/status.png)](https://drone.io/github.com/cjstehno/effigy/latest)
