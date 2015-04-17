
# Things to do

> This is just notes to myself - don't try to figure it out :-)

Field type mappings
    create 
    update
    retrieve
    delete
        
    (sql-insert, sql-update)?



================================================

* more documentation around the sql template language support
* document how users can modify the logger settings for the transforms

---------------------------------------------

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

consider removing the version/id updates on create - since other fields may be updated by sql and not transferred, it's probably best to just leave
everything to the client to call a retrieve method

? should Effigy provide/support any sort of validation framework
? should Effigy support query/result caching (spring-based)
? should Effigy support gmetrics around CRUD/SQL methods

/////////////////////////

! retrieve based on association criteria
! allow for cascade operations on associations (create, update, delete)?

//////////

FIXME: @SqlSelect support for Entities in mappers and where clauses (?)


/////////////

## New test schema

Room
    Long id
    Name name (number,label) 1:1
    byte capacity
    Location location (lat, lon) embedded
    Map<Scale,Image> images
    
Meeting
    id 
    version
    Date startDate
    Date endDate
    Room room (how does this translate)
    Person organizer
    Set<Person> attendees
    Collection<Feature> features
    
Feature
    id
    Feature.Type (enum) type
    String description
    
Person
    id
    version
    Name name (first, middle, last) embedded
    CompanyId (some custom type like a serialized obj or something)
    
Image
    id
    byte[] content
    int width
    int height
    Scale scale (enum)
    String contentType