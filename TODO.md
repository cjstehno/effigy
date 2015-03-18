
# Things to do

> This is just notes to myself - don't try to figure it out :-)

* Add support for naked non-entity field types auto-mapping to Embedded
* Add support for collection fields without annotations
* Add support for @Transient fields - to be ignored by Effigy inspections
* Clean up the groovydoc format so that its readable - also generate docs for site
* more documentation around the sql template language support
* Add support for the retrieve(create()) use case - maybe if the return type of a @Create is the Entity type bake this into the generated code

* Refactor the logging - the @Log annotation work but need to figure out how to get logging configured in my context (ast)

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

//////////

FIXME: @SqlSelect support for Entities in mappers and where clauses (?)

FIXME: allow for sql to be resolved from external source (like properties, or some other file ) - compile time or runtime?


mapper and extractor builder dsl (?)
update copyrights

////////////

* better separation of internal vs external api
* bring standard tests into main project and keep test as a full test project (spring)
* copy the codenarc and tests from "test" project into site
* consider alternative jdbc strategy (non-spring) - configurable switching with annotation @JdbcStrategy(Spring|Groovy) - seems like a waste of time also dbutil.
* stored procedure support
* batch support