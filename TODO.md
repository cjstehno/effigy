
# Things to do

> This is just notes to myself - don't try to figure it out :-)

* Add support for naked non-entity field types auto-mapping to Embedded
* Add support for collection fields without annotations
* Add support for @Transient fields - to be ignored by Effigy inspections
* allow specification of jdbctemplate bean name for autowire, also add it as a property rather than field to allow getter/setter
* Clean up the groovydoc format so that its readable - also generate docs for site
* more documentation around the sql template language support
* Add support for the retrieve(create()) use case - maybe if the return type of a @Create is the Entity type bake this into the generated code

* Add groovydocs to the userguide/site (may need a task to generate both and save off generated content)

FIXME: @Delete methods need to support map with property fields as input parameter
FIXME: @Count methods need to support map with property fields as input parameter
FIXME: @Exists methods need to support map with property fields as input parameter
FIXME: @Retrieve methods need to support map with property fields as input parameters
FIXME: @Retrieve support for runtime sort order param
FIXME: @Retrieve support limit with association queries
FIXME: @Retrieve support offset with association queries

FIXME: there are issues with primitive long id fields
FIXME: mapped assocations require an instance of their collection by default - should allow null by default

* gmetrics reporting might be interesting
* support for batch operations in CRUD and SQL annotation methods
* support for execute operations in sql annotations e.g. @SqlExecute - needed?

////

@Association
@Collected(ordering=[@ListOrdering(property='lastName', direction=DESC)])
List<Person> owner = []

/////

consider removing the version/id updates on create - since other fields may be updated by sql and not transferred, it's probably best to just leave
everything to the client to call a retrieve method

Should Effigy provide any kind of validation and/or validation hooks?
- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)

support for result caching - basically integrate with the spring caching mechanism (optional)

///////////

Enhanced property handling in the sql template language - need ability to do sql template operations on embedded/component and maybe associations

@property.@property
@name.@firstName

:entity.@name.@lastName - sql gets the ? while the param gets the property lookup



* ability to create and retrieve entity
    Job job = jobRepository.create(attributes:[user:'admin'])
* support for large text/CLOB/BLOB fields
* ability to supply custom field serializer/deserializer @Column(handlerName='', handlerClass='') - stateless class to read/write value (name allows to pull from spring)
    - I dont think this would be something Id want at runtime, just compile time

Come up with a better way to test the functionality

/////////////////////////

Person
    Name (first, middle, last)
    dob
    married
    gender (male|female)
    EmploymentInfo (title, salary) 1:1
    Map<label, Address> addresses
    List<Appointment> appointments

Appointment
    start
    end
    description
    Set<Person> attendees
    Room room

Room
    name
    capacity

! retrieve based on association criteria
! allow for cascade operations on associations (create, update, delete)?

///////////////////////////
///////////////////////////

consider the name replacement code in spring to let it do the work

@SqlSelect('select a,b,c, from foo where id=:id)
@RowMapper(clazz=AbcRowMapper) - injects code to create a shared row mapper in the repository, based on this row mapper class
@RowMapper(bean='abcRowMapper') - injects code to autowire in a shared row mapper with the given bean name (property not field)
@RowMapper(clazz=AbcRowMapper, factory='rowMapper') - injects code to call static factory method of factory to create mapper instance
AbcObj findById(Long id)
Collection<AbcObj> ...
List<AbcObj>

consider also counts, single fields, etc

* could allow factory method to be an instance method on the bean similar to static on class (any use in this?)

register a default set of retrun type mappers for things like int, long, Date, etc - would be nice if there were a means 
for developers to register their own (of course they could just configure a mapper)
- entity types row mappers, extractors should be registered by default (maybe just for the repository entity)
- maybe allow an annotation on repository class to set return type handling on all SqlSelect annotated methods in that repository.
- @TypeRowMapper(type=Integer, clazz|bean|(clazz & factory)) - similar for extractor and others
    - this would be useful if you have a bunch of custom methods with same mapper (return type)


@SqlSelect (transform - any method of @Repository annotated class)
    sql='select a,b,c from foo where x=? and y=? limit 10'
    supporting-annotations: PreparedStatementSetter, RowMapper, ResultSetExtractor
        - if no "mapping" annotation provided, will allow return Collection of
            Entity - will use the entity row mapper (or extractor as appropriate)
            Map - will return the values as map name-value pairs
            Pogo - will use simple bean property row mapper
    method-return: single object or collection (should be based on extractor or mapper)
        - maybe defaults to a map or something (or a simple object mapper)
    method-params: sql params by name or position (type is used) or pogo/entity (if entity specified)
        - limit offset, order defined as standard input params

    -with entity-
    select @name,#id,@age from #table where @name=:name'
    which will work similar to the entity versions of the annotations but with less "magic" basically what you get is what you put in the sql this just allows you 
    some custom queries based on pogos or entities obviously the #id and #table would only work for entities

start with generic support then consider adding special support for entities as return values
allow for sql to be resolved from external source (like properties, or some other file ) - compile time or runtime?

variants: 
    return type & (single | collection ) - special handling for entity type?
    input params (type order and name) : used as sql replacement params
    mapper, extractor

- if mapper/extractor defined, use it, otherwise determine mapper/extractor based on return type (probably just mapper), or fail compilation

///

@SqlUpdate (transform - any method of @Repository annotated class)
    sql='insert into foo (a,b,c) values(?,?,?)'
    sql='update foo set a=?, b=?, c=? where d=?'
    sql='delete from foo where a=:ahe and c=:cee'
    entity - same as above
    supporting-annotations: PreparedStatementSetter
    method-return: boolean or count based on return type
    method-params: sql params by name or position (type is used) or pogo/entity (if entity specified)

    PreparedStatementSetter (if used) should have access to the method params - I might need to have a custom base interface or something (MethodParamStatementSetter). I guess it could be optional since you could then decide whether or not you care about the params.


@RowMapper (normal method annotation - not with extractor)
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class

@ResultSetExtractor (normal method annotation - not with mapper)
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class

@PreparedStatementSetter
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class


