
# Things to do

* Add association support for Map type in entities
* Add support for naked non-entity field types auto-mapping to Embedded
* Add support for collection fields without annotations
* Add support for @Transient fields - to be ignored by Effigy inspections

FIXME: @Delete methods need to support map with property fields as input parameter
FIXME: @Count methods need to support map with property fields as input parameter
FIXME: @Exists methods need to support map with property fields as input parameter
FIXME: @Retrieve methods need to support map with property fields as input parameters
FIXME: @Retrieve support for runtime sort order param
FIXME: @Retrieve support limit with association queries
FIXME: @Retrieve support offset with association queries

support for generic INSERT,UPDATE, DELETE, SELECT statements no based on entities
@SqlInsert, @SqlUpdate, ...

Should Effigy provide any kind of validation and/or validation hooks?
- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)

//////////////////////////////

@SqlSelect (transform - any method of @Repository annotated class)
    sql='select a,b,c from foo where x=? and y=? limit 10'
    entity=pogo may be @Entity or not used
    supporting-annotations: PreparedStatementSetter, RowMapper, ResultSetExtractor
    method-return: single object or collection (should be based on extractor or mapper)
    method-params: sql params by name or position (type is used) or pogo/entity (if entity specified)

    -with entity-
    select @name,#id,@age from #table where @name=:name'
    which will work similar to the entity versions of the annotations but with less "magic" basically what you get is what you put in the sql this just allows you some custom queries based on pogos or entities
    obviously the #id and #table would only work for entities


@SqlUpdate (transform - any method of @Repository annotated class)
    sql='insert into foo (a,b,c) values(?,?,?)'
    sql='update foo set a=?, b=?, c=? where d=?'
    sql='delete from foo where a=:ahe and c=:cee'
    entity - same as above
    supporting-annotations: PreparedStatementSetter
    method-return: boolean or count based on return type
    method-params: sql params by name or position (type is used) or pogo/entity (if entity specified)

    PreparedStatementSetter (if used) should have access to the method params - I might need to have a custom base interface or something (MethodParamStatementSetter). I guess it could be optional since you could then decide whether or not you care about the params.

It may be better design to have mappers, extractors and setters as additional annotations used to augment the two Sql* annotations. This would simplify them and keep functionality well-grouped

@RowMapper (normal method annotation - not with extractor)
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class

@ResultSetExtractor (normal method annotation - not with mapper)
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class

@PreparedStatementSetter
    beanName - bean name of object to be used
    className - bean class of object to be used
    factoryMethod - (with className) calls static method on class


also consider refactoring the sql and params into a single object so that you have a SqlSelect builder that takes something like

select.where('name = ?',varX('name'))

String sql = select.sql()
def params = select.params()

so that I dont have to keep track of where params are injected and used by the sql. Also, the different builders might be able to share more base/delegate code.