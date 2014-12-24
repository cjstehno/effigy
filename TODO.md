
# Things to do

- association support for maps (specify key or based on associated entity id)
- naked non-entity type field should auto-map to Embedded
- association support for collections without an annotation
! ensure that Association entities are Effigy Entity annotated

- rename the extractors to Entity.extractor() and collectionExtractor()
! decouple the create/update method injection code for associations

- add support for @Transient fields - to be ignored by Effigy inspections


- finder criteria extra information
    - limit
    - order
    - pagination
- finder complex/custom criteria
    @Criteria('name=? and birthDate between (x and ?)')
    List<Entity> findWhere(String name, Date birthDate)
    - take the default select sql and append based on param position or name
        name=:name and birth_date between (x and :birthDate)

    @OrderedBy(...) - compiled in
    findBySomething(something)

    findBySomething(something, @OrderBy orderBy) - specified at runtime

    findBySomething(something, @PageBy PageBy

    
- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)


