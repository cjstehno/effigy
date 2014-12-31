
# Things to do

- association support for maps (specify key or based on associated entity id)
- naked non-entity type field should auto-map to Embedded
- association support for collections without an annotation
! ensure that Association entities are Effigy Entity annotated

- rename the extractors to Entity.extractor() and collectionExtractor()
! decouple the create/update method injection code for associations

- add support for @Transient fields - to be ignored by Effigy inspections


UPDATED SQL ANNOTATION HANDLING


keep mapper and extractors
keep basic crud


@ - property name reference to a db column
# - macro that will be expanded based on content
: - sql statement placeholder

THESE SHOULD REPLACE THE EXISTINNG CRUD - just provide an inteface annotted properly to reproduce the desired
methods

methods in interface or abstract
parameter names/types are important
return types may be important

@Retrieve
  basically everything in sql after 'select * from TABLE...'
  return value should be a single entity or collection of entities of the enclosing repository type
  params are just replacement variables - if no sql provided they will be considered propertues and used to gen basic where clause
  need support for (in retrieve methods) : paging, ordering, limiting

@Retrieve('where @id=:id')
Person retrieve(id)

@Retrieve()
List<Person> retrieveAll()

@Retrieve('where @firstName=:firstName and @lastName=:lastName limit=:limit')
List<Person> findByName(String lastName, String firstName, int limit)


static offset, limit and ordering
@Retrieve(value='@firstName=:firstName and @lastName=:lastName', offset=0, limit=10, order="@lastName desc, @firstName desc")
List<Person> findByName(String lastName, String firstName)

@Retrieve(value='@firstName=:firstName and @lastName=:lastName')
List<Person> findByName(String lastName, String firstName, PageBy pageBy, OrderBy orderBy) - populates the offset, limit and order at runtime
List<Person> findByName(String lastName, String firstName, Limit limit, OrderBy orderBy) - populates the limit and order at runtime
List<Person> findByName(Map props, Limit limit, OrderBy orderBy) - populates the limit and order at runtime



@Retrieve('where @firstName=:firstName and @lastName=:lastName #pageBy order by @lastName, @firstName desc')
List<Person> findByName(String lastName, String firstName, PageBy pageBy)
  #pageBy --> offset=:offset limit=:limit (values pulled from pageBy object or offset limit params)

@Retrieve('where @firstName=:firstName and @lastName=:lastName #pageBy #orderBy')
List<Person> findByName(String lastName, String firstName, OrderBy orderBy)

FIXME: @Delete methods need to support map with property fields as input parameter
FIXME: @Count methods need to support map with property fields as input parameter
FIXME: @Exists methods need to support map with property fields as input parameter

support for generic INSERT,UPDATE, DELETE, SELECT statements no based on entities
@SqlInsert, @SqlUpdate, ...



VALIDATION

- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)