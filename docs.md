> This is just a temporary dumping point...

objects used in Associations or as Components must be Effigy Entities themselves.


# OneToMany

This annotation is applied to an entity property to denote an association of one entity to many associated entities. The association is represented
by a collection of entities.

## Effects on CRUD operations

### Create

The associated entities must already exist - they will not be saved by association, and any unsaved relations will
the create operation to fail with an exception.

### Retrieve

The associated entities will be populated.

### Update

The associated entities must already exist; any unsaved relations will cause the update operation to fail with an
exception.

### Delete

The associations will be deleted, not the associated entities themselves.

# Embedded

# Component

This association is applied to an entity property to denote an association of one entity to one associated entity. The association is represented
by a single entity

The object itself will be managed by the enclosing entiy

... much like the OneToMany, the associations will be managed, not the actual objects

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