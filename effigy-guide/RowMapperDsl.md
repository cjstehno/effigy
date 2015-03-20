# RowMapper DSL

The `com.stehno.effigy.dsl.RowMapperDsl` class provides a DSL used to define and configure Spring RowMappers.

Given a couple of domain objects to be mapped from the database:

```groovy
@ToString
class InterestingObject {

    String partName
    Date someDate
    int lineNumber
    List<String> items
    EmbedddObject something
}

@ToString
class EmbedddObject {
    long id
    String label
}
```

a `RowMapper` may be created using the DSL as follows:

```groovy
RowMapper<InterestingObject> rowMapper = mapper(InterestingObject) {
    map 'partName'
    map 'someDate' using { x -> new Date(x) }
    map 'items' from 'line_items' using { x -> x.split(';') }
    map 'lineNumber' from 'line_number'
    map 'something' using mapper(EmbedddObject, 'obj_') {
        map 'id'
        map 'label'
    }
}
```

Properties may be mapped in four methods:

```groovy
map 'partName'
map 'lineNumber' from 'line_number'
```

Where the property `partName` will be mapped from a database field named `part_name`. The default behavior when a "from" is not called is to
convert the property name to underscore-case to find the field name.

In some cases, the incoming data from the database must be transformed in some manner before being applied to the target object. This may be
accomplished with the "using" method call, which accepts a `Closure` or a `RowMapper`:

```groovy
map 'items' from 'line_items' using { x -> x.split(';') }
```

The `RowMapper` case allows a property to be populated from multiple columns of the database table (such as for an embedded object):

```groovy
map 'something' using mapper(EmbedddObject, 'obj_') {
    map 'id'
    map 'label'
}
```

The generated `RowMappers` are not compiled code, but simply a convenient builder construct. Due to this fact, they are not recommended in cases
where high-performance is desired.

> TBD: AST-based "compiled" RowMapper DSL...
