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

