# Entities

Entities in Effigy are defined and configured using annotations, the only required annotation being the `@Entity` annotation. The others are optional and used
to help configure the system to your needs.

> TBD: Document the expected DDL for each annotation.

## @Entity

The `@Entity` annotation is applied to an entity type, a POGO to denote that the object is an entity to be managed by Effigy.

The annotation has only one optional property, `table`, which is used to provide a custom name for the table containing the 
entity data. By default, if not specified, the table name will be the name of the entity with an 's' added to the end of it.

Other than marking an object as an entity, the annotation causes some helper classes to be generated for the entity; a `RowMapper`
and two `ResultSetExtractor` implementations will be generated for each entity object. Each will be named with the prefix of the 
class name of the entity. As an example for the entity class `Person` you would have three new classes:

    PersonRowMapper
    PersonAssociationExtractor
    PersonCollectionAssociationExtractor

Each will be created in the same package as the entity itself. Also helper methods will be added to the entity class to retrieve
instances of each of these mappers and extractors. For the example above these would have the following signatures:

    static PersonRowMapper rowMapper(String prefix='')
    static PersonAssociationExtractor associationExtractor()
    static PersonCollectionAssociationExtractor collectionAssociationExtractor(Integer offset=null, Integer limit=null)

These mappers, extractors and helper methods are available for use by classes outside of Effigy itself, however, they are used extensively by internal code.

## @Column

The `@Column` annotation is an optional one applied to the entity properties. It is used to provide an alternate name for the database column associated
with the property. By default, if this annotation is not used, the property name will be converted from camel-case to underscore-case (e.g. firstName 
becomes first_name).

## @Id

The `@Id` annotation is applied to an entity property that is to be defined as the unique identifier for an instance of the entity. While an id is not
explicitly required by Effigy, you will not benefit from most of the functionality without one. An entity id can be a property of any Object type, though
generally a Long is recommended.

The generating and management of ids is handled by your code and/or the database itself, Effigy does not take any action to generate ids.

Currently, Effigy does not support multiple properties annotated with the `@Id` annotation.

## @Version

The `@Version` annotation is an optional annotation applied to en entity property that is to be defined as the Optimistic versioning property for the entity. 
The type of the property must be a Long, long, Integer or int.

The generated update methods (see documentation for the `@Update` annotation) will make use of the version property to determine whether or not the 
update is allowed, based on the version being updated compared to the version in the database.

Currently, Effigy does not support multiple properties annotated with the `@Version` annotation.

## @Embedded

The `@Embedded` annotation is used to denote an embedded component property. The properties of the embedded object are mapped to fields of the entity table. 
The database columns are named as specified by the embedded object property names following the standard name conversion rules, as well as any `@Column` 
annotations. Also, a prefix will be prepended to the column names which will be the same as the name of the embedded property in the enclosing entity. The 
annotation accepts an optional `prefix` property which allows the prefix to be specified explicitly.

The type of the embedded object may be an entity; however, it need not be. If the embedded object is an entity, any @Id or @Version annotations will **not** be 
honored, since the table data is contained in the table for the enclosing entity. The `@Column` annotation will be honored.

> TBD: support for un-annotated embedded types?

## @Component

The `@Component` annotation is used to denote that a property has a one-to-one relationship with the enclosing entity. The one-to-one assocation expressed 
by a Component is similar to that of an `@Embedded` object except that this relationship is contained in a separate lookup table, rather than being embedded 
within the same table as the entity data.

The type of the annotated field may be an Entity-annotated class; however, the entity `table` property as well as any `@Id` or `@Version` annotated fields 
will be ignored by the component association functionality.

The `@Component` annotation supports two configuration properties.

* The `lookupTable` property is used to specify the name of the lookup table used to link the component table with the entity table. If this property is not 
specified, the table will be named as the table name of the component type.
* The `entityColumn` property is used to specify the name of the field used to identify the owning entity in the associated table. If this property is not 
specified, the name will be the table name of the owning entity suffixed with '_id'.

> TBD: support for un-annotated component types?

## @Association

The `@Association` annotation is used to denote that a collection property (Collection or Map implementation) represents an association between the enclosing
entity and another entity. The collection contents must be a type annotated with `@Entity`.

The `@Association` annotation supports three optional properties:

* The `joinTable` property is used to specify the name of the association reference table. If this property is not specified, the table name will be the entity
table name and the name of the entity association property separated by an underscore.
* The `entityColumn` property is used to specify the name of the id column for the owning entity. By default, the entity table name will be used, suffixed with '_id'.
* The `assocColumn` property is used to specify the name of the associated entity id. By default, the associated entity table name will be used, suffixed with '_id'.

The CRUD annotations used by `@Repository` annotated classes will properly handle operations with associations, as will the `RowMapper` and `ResultSetExtractor`
implementations.

> TBD: support for un-annotated associations.

### @Mapped Association

Map associations are supported without additional annotations; however, the `@Mapped` annotation may be added in order to specify the property to be used as 
the Map key. If the `keyProperty` is not specified, the map key will be the id property of the associated entity.

Care must be taken when using mapped associations - at this time there is no checking around the type of key used. It is on the developer to ensure that the
keys used are unique and defined in such a way that they will be appropriate for use as Map keys.


