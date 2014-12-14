
# Things to do

! ensure that Association entities are Effigy Entity annotated

! decouple the create/update method injection code for associations

! Repository annotation should add springs @Repository annotation to impl class

! unit tests for row mappers and extractors

- add count/exists methods, and maybe a findOne that does not throw exception if not found

* implement @Ignored (ignored field) - or Transient

* support manytomany
* support maps as asociation containers

* support for finders

* test in real project scenarios


onetoone
x    create
x    retrieve
    update
x    delete
x    mapper
x    extractor

@OneToOne Address home




------------


1-1

SomeType someField

type may be generic or may be an entity
relationship managed via relation table
recognized by field not being one of the supported types and not collection, or annotation

@Association(joinTable='foo', entityColumn='entity_id', assocColumn='assoc_id')

crud operations will manage the relationships, not the actual entities


*-1, *-*

Collection impls, or Map

associated type may be generic or entity
relationship managed via relation table
recognized by field being a collection or map, or annotation

@Association(joinTable='foo', entityColumn='entity_id', assocColumn='assoc_id')

crud operations will manage the relationships, not the actual entities

------

May also want to note about using annotations for building spring validate construct