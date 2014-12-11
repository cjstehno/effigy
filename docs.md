> This is just a temporary dumping point...


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

# OneToOne

This association is applied to an entity property to denote an association of one entity to one associated entity. The association is represented
by a single entity

## Create

The associated object will be created if populated.

## Retrieve

The associated object will be retrieved with the main entity.

## Update

The association will be updated, created or deleted as required by the update state of the enclosing entity.

## Delete

The associated object will be deleted with its parent entity.

