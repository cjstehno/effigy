# Introduction

## What Effigy Is

Effigy is an annotation-driven utility layer on top of the Spring-JDBC API providing additional functionality
based on standard usage patterns and scenarios. It provides a simple means of generating CRUD methods with
simplicity inspired by the Spring-Data JPA project.

## What Effigy Is Not

Effigy is NOT a full ORM, think of it more akin to an object mapping framework. Effigy is NOT a replacement for a true
ORM, if that is what you want or what you need.

## Why Effigy Exists

Why another database-related framework and isn't this just "reinventing the wheel"? I don't think so. When I first
started thinking about what would become Effigy, I was inspired by a presentation about Spring-Data JPA and my past
experience with Grails' Gorm library. I was working on a small to moderate project with fairly simple database
requirements, but when I tried to use the "standard" libraries like Hibernate with or without JPA it seemed that there
were a lot of configuration requirements and it seemed that I had to conform my project to their requirements - unless
I wanted even more pain. As I got deeper I ran into roadblock after roadblock where I found myself thinking how easy
these would be if I could just use plain old JDBC. Yes, you can do that to some degree in Hibernate, but there are costs.
I think Effigy fills the gap between hand-coded JDBC and a full ORM framework. It's not for every situation, but it's
another tool in the toolbox.

## How the Sausage Is Made

Effigy makes use of Groovy's AST transformation support so that the annotations in your entities and repositories are
used to generate the required code at compile-time rather than at runtime (or as proxies). This limits the use of Effigy
to Groovy-based projects; however, that was a trade-off I was willing to make.

Under the covers, the JDBC operations boil down to calls to auto-injected instances of the Spring Framework `JdbcTemplate`
class. This ties the project to Spring, at least the spring-jdbc library. Again, this was an easy choice since I use it for
all my personal projects anyway.
