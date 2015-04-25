# Entities

Entities in Effigy are defined and configured using annotations, the only required annotation being the `@Entity` annotation. The others are optional and used
to help configure the system to your needs.

## @Entity

The `@Entity` annotation is applied to an entity type, a POGO to denote that the object is an entity to be managed by Effigy.

The annotation has only one optional property, `table`, which is used to provide a custom name for the table containing the 
entity data. By default, if not specified, the table name will be the name of the entity with an 's' added to the end of it.

Other than marking an object as an entity, the annotation causes some helper classes to be generated for the entity; a `RowMapper`
and two `ResultSetExtractor` implementations will be generated for each entity object. Each will be named with the prefix of the 
class name of the entity. As an example for the entity class `Person` you would have three new classes:

* `PersonRowMapper` - maps a single row of the person entity table to a single `Person` object instance (no associations).
* `PersonAssociationExtractor` - maps a single row of the person entity table to a single `Person` object instance (with associations).
* `PersonCollectionAssociationExtractor` - maps a collection of rows of the person entity table to a collection of `Person` instances (with associations).

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

The `@Column` annotation also provides an optional `type` property which allows the SQL type of the column to be explicitly specified as one of the 
`java.sql.Types` value constants (Note: _only_ raw int values or references to `java.sql.Types` values will work due to complexities around AST annotation
 processing). If no value is provided, the default set of simple Java-to-SQL type mappings will be used.

The `@Column` annotation provides an optional `handler` property. This property allow the developer to specify a Class which will be used to convert 
the column property value to and from the database value. The class is expected to have two static methods:

```groovy
static ENTITY readField(DATABASE db_value)
```

Which will be used to convert the database field value to that required by the entity property object.

```groovy
static DATABASE writeField(ENTITY domainValue)
```

Which will be used to convert the entity property object value to that required by the storage database.

These methods should be stateless and the conversion types on both sides of the read/write should match what is expected by the entity or database respectively.

## @Id

The `@Id` annotation is applied to an entity property that is to be defined as the unique identifier for an instance of the entity. While an id is not
explicitly required by Effigy, you will not benefit from most of the functionality without one. An entity id can be a property of any Object type, though
generally a Long is recommended.

The generating and management of ids is handled by your code and/or the database itself, Effigy does not take any action to generate ids. The DDL expected
could be something similar to the following:

```sql
id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY
```

Currently, Effigy does not support multiple properties annotated with the `@Id` annotation.

## @Version

The `@Version` annotation is an optional annotation applied to en entity property that is to be defined as the Optimistic versioning property for the entity. 
The type of the property must be a Long, long, Integer or int.

The generated update methods (see documentation for the `@Update` annotation) will make use of the version property to determine whether or not the 
update is allowed, based on the version being updated compared to the version in the database. The DDL expected could be something similar to the following:

```sql
version BIGINT NOT NULL,
```

Currently, Effigy does not support multiple properties annotated with the `@Version` annotation.

## @Transient

The `@Transient` annotation can be applied to an entity property to denote that the property will be ignored by the effigy processing framework. The 
property will not participate in any database code generation.

## @Embedded

The `@Embedded` annotation is used to denote an embedded component property. The properties of the embedded object are mapped to fields of the entity table. 
The database columns are named as specified by the embedded object property names following the standard name conversion rules, as well as any `@Column` 
annotations. Also, a prefix will be prepended to the column names which will be the same as the name of the embedded property in the enclosing entity. The 
annotation accepts an optional `prefix` property which allows the prefix to be specified explicitly.

The type of the embedded object may be an entity; however, it need not be. If the embedded object is an entity, any @Id or @Version annotations will **not** be 
honored, since the table data is contained in the table for the enclosing entity. The `@Column` annotation will be honored.

The DDL expected for an embedded property is a set of prefixed columns in the main entity table. Consider the `Person` case, where there is an embedded
property `Name name`, the framework would expect the following:

```sql
CREATE TABLE people (
  -- other fields omitted
  name_first    VARCHAR(25)           NOT NULL,
  name_middle   VARCHAR(25)           NOT NULL,
  name_last     VARCHAR(25)           NOT NULL
);
```

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

The DDL expected for components is basically a 1-1 lookup table. Consider the case where a `Person` has a work address component defined as:

```groovy
@Component(lookupTable = 'employers') Address work
```

This would map the component to an `employers` table, by the id of the person, such as:

```sql
CREATE TABLE employers (
  people_id BIGINT REFERENCES people (id),
  line1     VARCHAR(30),
  line2     VARCHAR(30),
  city      VARCHAR(20),
  state     VARCHAR(2),
  zip       VARCHAR(10)
);
```

> TBD: support for un-annotated component types?

## @Association

The `@Association` annotation is used to denote that a collection property (Collection or Map implementation) represents an association between the enclosing
entity and another entity. The collection contents must be a type annotated with `@Entity`.

The `@Association` annotation supports three optional properties:

* The `joinTable` property is used to specify the name of the association reference table. If this property is not specified, the table name will be the entity
table name and the name of the entity association property separated by an underscore.
* The `entityColumn` property is used to specify the name of the id column for the owning entity. By default, the entity table name will be used, suffixed with '_id'.
* The `assocColumn` property is used to specify the name of the associated entity id. By default, the associated entity table name will be used, suffixed with '_id'.

The DDL expected for associations is that of an association table used to store the relationship between the two entities. Consider a `Pet` object associated
with a `Person`, the relationship would be defined in the entity as:

```groovy
@Association(joinTable = 'peoples_pets', entityColumn = 'person_id', assocColumn = 'pet_id')
Set<Pet> pets = [] as Set<Pet>
```

The DDL required by this association would be something like:

```sql
CREATE TABLE peoples_pets (
  person_id BIGINT REFERENCES people (id),
  pet_id    BIGINT REFERENCES pets (id),
  UNIQUE (person_id, pet_id)
);
```

which would contain the relationship information.

The CRUD annotations used by `@Repository` annotated classes will properly handle operations with associations, as will the `RowMapper` and `ResultSetExtractor`
implementations.

> TBD: support for un-annotated associations.

## @Mapped Association

Map associations are supported without additional annotations; however, the `@Mapped` annotation may be added to a property annotated with `@Association`
in order to specify the property to be used as the Map key. If the `keyProperty` is not specified, the map key will be the id property of the associated entity.

Care must be taken when using mapped associations - at this time there is no checking around the type of key used. It is on the developer to ensure that the
keys used are unique and defined in such a way that they will be appropriate for use as Map keys.

The DDL expected for mapped associations is the same as that for collection associations.
