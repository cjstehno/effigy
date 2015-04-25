# Getting Started

## Support and Requirements

Effigy is built using `Groovy 2.4.0` on top of `Java 8`. The tests are all run against an H2 database and I have also used it
to develop under a PostgreSql database. The generated SQL is intended to be standard so the framework should work against any
standard SQL database. I would be happy to list any others - if you are using a different database, let me know and it will be
listed.

## Setup for Development

If you want to develop features or fix bugs in the Effigy library, clone the repository and run:

    ./gradlew build

There no extra configuration or dependencies required at this time.

## Setup for Use

If you want to use the Effigy library in your project you will need to add the `effigy-core` library as a dependency and then
read the rest of this guide (and probably the unit tests) to learn how the annotations are used.

Currently only pre-release builds are available on Bintray. You will need to add a reference to the repository; for Gradle, this would be

    repositories {
        maven { url 'http://dl.bintray.com/cjstehno/stehno' }
    }

Alternately, you can build it yourself and install it locally using `mavenLocal()` in your Gradle dependencies. You can build and publish the
development artifact using:

    ./gradlew build publishToMavenLocal

Then add it to your dependencies:

    com.stehno.effigy:effigy-core:0.4.0

> The most stable method at this point is to build it yourself, since the current codebase may or may not be reflected in the published build.


