
# Things to do

- association support for maps (specify key or based on associated entity id)
- naked non-entity type field should auto-map to Embedded
- association support for collections without an annotation
! ensure that Association entities are Effigy Entity annotated

- rename the extractors to Entity.extractor() and collectionExtractor()
! decouple the create/update method injection code for associations

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