> This is just a temporary dumping point...


# OneToMany

This annotation is applied to an entity property to denote an association of one entity to many associated entities.

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
