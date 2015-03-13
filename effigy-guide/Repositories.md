# Repositories

Repositories in Effigy are denoted using the `@Repository` annotation. The Effigy framework may be used without any defined repositories; however, they 
do provide additional functionality related to entities.

## @Repository

The `@Repository` annotation is applied to a class that is to be used as a repository for an Effigy entity. The `value` property of the annotation
is used to specify the type of entity being managed by the repository. The type specified must be annotated with the `@Entity` annotation. The `value`
property is required if CRUD operation annotations are to be used by the repository; however, the `value` may be omitted if only the SQL-based annotations
will be used.

When the `@Repository` annotation is applied to a repository class, the SpringFramework `@org.springframework.stereotype.Repository` annotation will be 
added to the compiled class This allows Spring component scanning to pick up Effigy repositories without any extra configuration.

The annotation also injects a property for a Spring `JdbcTemplate` instance into the class, with the signature:

    @Autowired JdbcTemplate jdbcTemplate

This allows the Spring auto-wiring to inject the configured Spring `JdbcTemplate` used by the project. This is the `JdbcTemplate` instance which will be
used by the operations in the repository.

If you do not want the `JdbcTemplate` to be autowired, you can specify the `autowired` property of the `@Repository` annotation as `false`.

Also, if you need to specify the name of the `JdbcTemplate` bean instance to be autowired, you can specify a value for the `qualifier` property of the
`@Repository` annotation. This will cause a `@Qualifier` annotation to be added to the `JdbcTemplate` property with the specified bean name as its
`value`. The `qualifier` property is ignored if the `autowired` property is set to `false`.

## CRUD Annotations

Classes annotated with `@Repository` are allowed to create their own data access methods and even use the Effigy-built `RowMapper`s and `ResultSetExtractor`s; 
however, Effigy also provides a set of helper annotations which allow simple generation of common CRUD operations.

All of the CRUD operations provided by the annotations will properly handle all features of an Effigy entity (assocations, components, embedded objects, etc).

If these annotations are used, the class should be defined as `abstract` and the annotated methods should also be `abstract` - these limitations may be 
relaxed in future releases. Also note that these suggestions are mainly required to make your IDE happy so you may want to experiment for your own needs.

### @Create

The `@Create` annotation is used to denote that a method is used to create and persist new entities.

A "create" method must accept as parameters, one of the following:

* an object of the entity type containing the data for the entity being created
* a Map<String,Object> instance containing the properties corresponding to an entity object
* individual properties of the entity to be created - the parameter name and type must match the target property of the entity.

A "create" method must return the identity type for the entity.

### @Retrieve

The `@Retrieve` annotation is used to denote a "retrieve" method in an Effigy repository.

A "retrieve" method must accept as parameters, one of the following:

* no parameters, to denote the retrieval of all entities of the managed type
* a Map<String,Object> object containing the properties corresponding to an entity - the parameter name and type must match the target property of the entity.
* individual properties of the entity to be deleted - the parameter name and type must match the target property of the entity.

Additionally, a "retrieve" method may except either or both of the following (though only one of each):

* a `@Limit` annotated `int` parameter used to limit the query results returned
* an `@Offset` annotated `int` parameter used to offset the start of the returned results

A "create" method must return one of the following:

* a `Collection` (or extension of) of the entity managed by the repository
* a single instance of the entity managed by the repository

The `@Retrieve` annotation also supports additional optional properties:

The `value` property is used to specify the retrieval criteria portion of the SQL using the Effigy SQL Template language. If no value is specified, the method 
parameters will be used as entity properties to build a default criteria statement.

The `offset` properety is used to define the select offset value to be used. Unlike the `@Offset` annotation, the value of the offset provided will be compiled
into the code, rather than determined at runtime.

The `limit` properety is used to define the select limit value to be used. Unlike the `@Limit` annotation, the value of the limit provided will be compiled
into the code, rather than determined at runtime.

The `order` properety is used to define the ordering Sql Template used by the query, basically just the order by clause, for example:

    @Retrieve(order='@lastName asc, @firstName asc')
    Collection<Person> findPeople()
    
See the SQL Template documentation for more information about the sytax.

#### @Offset

The `@Offset` annotation is used to annotate an `int` parameter of a method annotated with the `@Retrieve` annotation. It denotes that the provided value will be used 
as the runtime-determined offset of the query results. 

#### @Limit

The `@Limit` annotation is used to annotate an `int` parameter of a method annotated with the `@Retrieve` annotation. It denotes that the provided value will be used 
as the runtime-determined limit of the query results. 

### @Update

The `@Update` annotation is used to denote is used to update existing entities in the database.

An "update" method must accept one of the following: 

* an entity instance
* a Map<String,Object> containing entity properties to be updated - the parameter name and type must match the target property of the entity.

An "update" method must have a return type of one of the following:

* void
* a boolean denoting whether or not a change was made (based on updated row count)
* an int value representing the number of entities (updated row count)

### @Delete

The `@Delete` annotation is used to denote a delete method in an Effigy repository.

A "delete" method must accept as parameters, one of the following:

* no parameters to denote deleting all entities
* a Map<String,Object> containing the properties corresponding to an entity object - the parameter name and type must match the target property of the entity.
* individual properties of the entity to be deleted - the parameter name and type must match the target property of the entity.

A "delete" methods must return either:

* a boolean denoting whether or not something was actually deleted (based on deleted row count)
* an int denoting the number of items deleted (deleted row count)

The `@Delete` annotation also supports a `value` property which accepts a String containing an Effigy SQL Template. The template value is used to specify
the deletion criteria portion of the SQL (basically the where clause). If no value is specified, the method parameters will be used as entity properties 
to build a default criteria statement. An example would be:

    @Delete('@age <= :minAge or @age >= :maxAge')
    abstract int deleteOutsideAgeRange(int minAge, int maxAge)

which would delete all entities whose "age" property value falls outside the specified min and max values. See the SQL Template documentation for more details
about syntax and supported functionality.

### @Exists

The `@Exists` annotation is used denote an "exists" method in an Effigy repository.

An "exists" methods must accept as parameters, one of the following:

* no parameters, which will determine whether or not there are any instances of the target entity type
* a Map<String,Object> containing the properties corresponding to an entity - the parameter name and type must match the target property of the entity.
* individual properties of the entity - the parameter name and type must match the target property of the entity.

An "exists" method must return:

* a boolean to denote existence

The `@Exists` annotation also supports a `value` property which accepts a String containing an Effigy SQL Template. The template value is used to specify
the search criteria portion of the SQL (basically the where clause). If no value is specified, the method parameters will be used as entity properties 
to build a default criteria statement. See the SQL Template documentation for more details.

### @Count

The `@Count` annotation is used to denote a "count" method in an Effigy repository.

A "count" method must accept as parameters, one of the following:

* no parameters to denote counting all entities
* a Map<String,Object> containing the properties corresponding to an entity object - the parameter name and type must match the target property of the entity.
* individual properties of the entity - the parameter name and type must match the target property of the entity.

A "count" method must return and int denoting the number of items counted.
 
The `@Count` annotation also provides an optional `value` property, which is used to specify the counting criteria portion of the SQL using the Effigy SQL 
Template language. If no value is specified, the method parameters will be used as entity properties to build a default criteria statement. See the SQL 
Template documentation for more details about syntax and supported functionality.

## SQL Annotations

Classes annotated with `@Repository` are allowed to create their own custom data access methods using a set of helper annotations which allow simple 
generation of code for common SQL operations. Unlike the CRUD Annotations, these allow the developers to specify their own SQL statement to be used.

If these annotations are used, the class should be defined as `abstract` and the annotated methods should also be `abstract` - these limitations may be 
relaxed in future releases. Also note that these suggestions are mainly required to make your IDE happy so you may want to experiment for your own needs.

### @SqlSelect

The `@SqlSelect` annotation is used to annotate a method of a repository as a custom SQL query.

A "select" method may accept any type or primitive as input parameters; however, the name of the parameter will used as the name of the replacement
variable in the SQL statement, so they will need to be consistent.

A "select" method must return a single type or collection of a type that is appropriate to the `RowMapper` or `ResultSetExtractor` being used. If a `RowMapper`
or `ResultSetExtractor` are not specified, Effigy will attempt to resolve the appropriate mapper based on the return type - if it cannot resolve a
mapper an instance of the `BeanPropertyRowMapper` class will be used - this may or may not work for the configured scenario, but it is a minimal
fallback point.

> The return types currently supported by the default row mappers are the following: Byte, byte, Character, char, Short, short, Integer, int, Long,
long, Float, float, Double, double, Boolean, boolean, and String. Any other return types will fallback to use the BeanPropertyRowMapper.

The `value` property of the annotation is used to provide the SQL string which will be compiled into the method. The method parameters will be used as replacement
variables in the SQL using the parameter name prefixed with a colon (e.g. `:firstName`).

A "select" method using the default return type mapper resolution would look something like the following:

```groovy
@SqlSelect('select count(*) from people where age >= :min and age <= :max')
int countByAgeRange(int min, int max)
```

In order to configure a custom `RowMapper` or `ResultSetExtractor`, it must be specified using a secondary annotation: the `@RowMapper` or `@ResultSetExtractor`
annotations.

#### @RowMapper

The `@RowMapper` annotation is used with a `@SqlSelect` annotation to provide information about the `RowMapper` to be used.

There are three distinct configuration scenarios for mapper annotations, they can be defined by the annotations `bean`, or `type` properties, or by
a combination of the `type` and `factory` properties.

The `bean` property will inject code into the repository to autowire a reference to the bean with the specified name. The mapper bean must be defined
somewhere in the Spring context, and must implement the `RowMapper` interface. This bean will then be used as the `RowMapper` for the query.

The `type` property will inject code into the repository to use an instance of the specified class as the mapper. The class must implement the `RowMapper`
interface.

The `type` and `factory` properties used together will inject code that will call the static factory method on the specified class to retrieve an
implementation of `RowMapper` which will be used by the query.

If multiple properties are configured outside the scope of these scenarios, the precedence order will be `bean`, then `type`; `factory` will be ignored
if the `type` property is not specified.

The `singleton` property is used to specify whether or not the generated mapper is shared across multiple calls (singleton=true) or has a new instance
created for each use (singleton=false); the default is true. One thing to make special note of, is that for the case when the `bean` property is
specified along with the `singleton` property having a value of `false`, the configured bean in your application context should be configured as a
prototype bean, otherwise you are not really getting a new instance with each call.

Configured `RowMapper` instance are allowed to access the arguments passed into the method; to do this, the `arguments` property must be set to `true`,
which will force the `singleton` property to be `false` (prototype). See the description of the `singleton` property above for more information. Mappers
that accept method arguments must either implement the `ArgumentAwareHelper` or provide a method with the following signature:

```groovy
void setMethodArguments(Map<String,Object> args)
```

The arguments will be injected at runtime using this method. It is up to the implementation to make proper use of them. Obviously, mappers making use
of this construct are no longer stateless.

An example of using the `@RowMapper` annotation would be the following:

```groovy
@SqlSelect('select a,b,c from some_table where d=:d and e < :e')
@RowMapper(type=AbcMapper)
Collection<Abc> findByDAndE(String d, int e)
```

#### @ResultSetExtractor

The `@ResultSetExtractor` annotation is used with a `@SqlSelect` annotation to provide information about the `ResultSetExtractor` to be used.

There are three distinct configuration scenarios for extractor annotations, they can be defined by the annotations `bean`, or `type` properties, or by
a combination of the `type` and `factory` properties.

The `bean` property will inject code into the repository to autowire a reference to the bean with the specified name. The extractor bean must be defined
somewhere in the Spring context, and must implement the `ResultSetExtractor` interface. This bean will then be used as the `ResultSetExtractor` for the query.

The `type` property will inject code into the repository to use an instance of the specified class as the extractor. The class must implement the `ResultSetExtractor`
interface.

The `type` and `factory` properties used together will inject code that will call the static factory method on the specified class to retrieve an
implementation of `ResultSetExtractor` which will be used by the query.

If multiple properties are configured outside the scope of these scenarios, the precedence order will be `bean`, then `type`; `factory` will be ignored
if the `type` property is not specified.

The `singleton` property is used to specify whether or not the generated extractor is shared across multiple calls (singleton=true) or has a new instance
created for each use (singleton=false); the default is true. One thing to make special note of, is that for the case when the `bean` property is
specified along with the `singleton` property having a value of `false`, the configured bean in your application context should be configured as a
prototype bean, otherwise you are not really getting a new instance with each call.

Configured `ResultSetExtractor` instances are allowed to access the arguments passed into the method; to do this, the `arguments` property must be set to `true`,
which will force the `singleton` property to be `false` (prototype). See the description of the `singleton` property above for more information. Extractors
that accept method arguments must either implement the `ArgumentAwareHelper` or provide a method with the following signature:

```groovy
void setMethodArguments(Map<String,Object> args)
```

The arguments will be injected at runtime using this method. It is up to the implementation to make proper use of them. Obviously, extractors making use
of this construct are no longer stateless.

An example of using the `@ResultSetExtractor` annotation would be the following:

```groovy
@SqlSelect('select a,b,c from some_table where d=:d and e < :e')
@ResultSetExtractor(type=AbcExtractor, factory='getExtractor')
Collection<Abc> findByDAndE(String d, int e)
```

Note that when an extractor is used, the return type of the method should match, or at least be compatible with the return type of the `ResultSetExtractor`
since the extractor is used to build the return value explicitly.

#### @PreparedStatementSetter

The `@PreparedStatementSetter` annotation is used with a `@SqlSelect` annotation to provide information about the `PreparedStatementSetter` to be used.
This is an optional annotation and may be used in conjunction with a `@RowMapper` or `@ResultSetExtractor` annotation.

There are three distinct configuration scenarios for setter annotations, they can be defined by the annotations `bean`, or `type` properties, or by
a combination of the `type` and `factory` properties.

The `bean` property will inject code into the repository to autowire a reference to the bean with the specified name. The extractor bean must be defined
somewhere in the Spring context, and must implement the `PreparedStatementSetter` interface. This bean will then be used as the
`PreparedStatementSetter` for the query.

The `type` property will inject code into the repository to use an instance of the specified class as the setter. The class must implement the
`PreparedStatementSetter` interface.

The `type` and `factory` properties used together will inject code that will call the static factory method on the specified class to retrieve an
implementation of `PreparedStatementSetter` which will be used by the query.

If multiple properties are configured outside the scope of these scenarios, the precedence order will be `bean`, then `type`; `factory` will be ignored
if the `type` property is not specified.

The `singleton` property is used to specify whether or not the generated setter is shared across multiple calls (singleton=true) or has a new instance
created for each use (singleton=false); the default is true. One thing to make special note of, is that for the case when the `bean` property is
specified along with the `singleton` property having a value of `false`, the configured bean in your application context should be configured as a
prototype bean, otherwise you are not really getting a new instance with each call.

Configured `PreparedStatementSetter` instances are allowed to access the arguments passed into the method; to do this, the `arguments` property must be
set to `true`, which will force the `singleton` property to be `false` (prototype). See the description of the `singleton` property above for more
information. Setters that accept method arguments must either implement the `ArgumentAwareHelper` or provide a method with the following signature:

```groovy
void setMethodArguments(Map<String,Object> args)
```

The arguments will be injected at runtime using this method. It is up to the implementation to make proper use of them. Obviously, mappers making use
of this construct are no longer stateless.

An example of using the `@PreparedStatementSetter` annotation would be the following:

```groovy
@SqlSelect('select a,b,c from some_table where d=:d and e < :e')
@ResultSetExtractor(type=AbcExtractor, factory='getExtractor')
@PreparedStatementSetter(type=DandESetter)
Collection<Abc> findByDAndE()
```

Note that when an setter is used, there are no method arguments (or they are ignored) unless the annotation has `arguments` set to `true`; see the
description of the `arguments` property above, for more information.

### @SqlUpdate

> TBD...

 * Annotation used to denote a custom SQL-based update method in an Effigy repository.
 *
 * An "update" method may accept any type or primitive as input parameters; however, the name of the parameter will
 * used as the name of the replacement variable in the SQL statement, so they will need to be consistent.
 *
 * An "update" method must have a return type of one of the following:
 * - void
 * - a boolean denoting a non-zero update record count (true) or 0 (false)
 * - an int or long denoting the updated record count.

also allows @PreparedStatementSetter...