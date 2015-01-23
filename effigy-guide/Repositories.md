# Repositories

Repositories in Effigy are denoted using the `@Repository` annotation. The Effigy framework may be used without any defined repositories; however, they 
do provide additional functionality related to entities.

## @Repository

The `@Repository` annotation is applied to a class that is to be used as a repository for an Effigy entity. The required `value` property of the annotation
is used to specify the type of entity being managed by the repository. The type specified must be annotated with the `@Entity` annotation.

When the `@Repository` annotation is applied to a repository class, the SpringFramework `@org.springframework.stereotype.Repository` annotation will be 
added to the compiled class This allows Spring component scanning to pick up Effigy repositories without any extra configuration.

The annotation also injects a field for a Spring `JdbcTemplate` instance into the class, with the signature:

    @Autowired private JdbcTemplate jdbcTemplate

This allows the Spring auto-wiring to inject the configured Spring `JdbcTemplate` used by the project. This is the `JdbcTemplate` instance which will be
used by the operations in the repository.

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

> TBD: pending functionality
