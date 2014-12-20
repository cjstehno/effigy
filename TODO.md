
# Things to do

- association support for maps (specify key or based on associated entity id)
- naked non-entity type field should auto-map to Embedded
- association support for collections without an annotation
! ensure that Association entities are Effigy Entity annotated

- rename the extractors to Entity.extractor() and collectionExtractor()
! decouple the create/update method injection code for associations

- add generation for other helper methods
    count()
    exists(entityId)

- add generation for finders/helpers
    findXYZ
    countXYZ
    existXYZ

- add support for @Transient fields - to be ignored by Effigy inspections

- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)


@FindOperations - should be part of CrudOperations collected transform, and as stand alone
! the various CRUD operations are somewhat interdependent, this should be documented

with the FindOperations annotation you get added code-defined finder methods based on interface or abstract methods of the repository

all methods of the name pattern "find[One|]By(propName)[And|Or|][propName]..." - mainly focus on the find prefix, which should be allowable from an interface or as an abstract method signature.

Entity findOneById(id) - like retrieve(id); however, no error if not exist
- allows findOne

List<Entity> findByName(String name) - select * from entity where name=?

List<Entity> findByName(String name, OrderBy orderBy=null, PageBy pageBy=null)
- you would most likely always want/need ordering when paging, or ordering alone.
- should probably return a PagedList implementation of List

List<Entity> findByNameAndAge(String name, int age, OrderBy orderBy=null, PageBy pageBy=null)

and, or - allowed

need ability to define more complex criteria, probably just as sql

@Criteria('name=? and birthDate between (x and ?)')
List<Entity> findWhere(String name, Date birthDate)
- take the default select sql and append based on param position or name
    name=:name and birth_date between (x and :birthDate)