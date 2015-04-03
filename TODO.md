
# Things to do

* more documentation around the sql template language support
* document how users can modify the logger settings for the transforms

---------------------------------------------

> This is just notes to myself - don't try to figure it out :-)

* Add support for naked non-entity field types auto-mapping to Embedded
* Add support for collection fields without annotations
* Add support for @Transient fields - to be ignored by Effigy inspections

* Add support for the retrieve(create()) use case - maybe if the return type of a @Create is the Entity type bake this into the generated code

FIXME: @Count methods need to support map with property fields as input parameter
FIXME: @Exists methods need to support map with property fields as input parameter
FIXME: @Retrieve methods need to support map with property fields as input parameters
FIXME: @Retrieve support for runtime sort order param
FIXME: @Retrieve support limit with association queries
FIXME: @Retrieve support offset with association queries

FIXME: mapped assocations require an instance of their collection by default - should allow null by default

* gmetrics reporting might be interesting


consider removing the version/id updates on create - since other fields may be updated by sql and not transferred, it's probably best to just leave
everything to the client to call a retrieve method

Should Effigy provide any kind of validation and/or validation hooks?
- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)

support for result caching - basically integrate with the spring caching mechanism (optional)


* ability to create and retrieve entity
    Job job = jobRepository.create(attributes:[user:'admin'])


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


* bring standard tests into main project and keep test as a full test project (spring)


