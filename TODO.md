
# Things to do

- association support for maps (specify key or based on associated entity id)
- naked non-entity type field should auto-map to Embedded
- association support for collections without an annotation
! ensure that Association entities are Effigy Entity annotated

- rename the extractors to Entity.extractor() and collectionExtractor()
! decouple the create/update method injection code for associations

- add generation for other helper methods
    count()
    exists(entityId)

- add generation for finders/helpers
    findXYZ
    countXYZ
    existXYZ

- add support for @Transient fields - to be ignored by Effigy inspections

- annotation-driven validation support hooks
    length(min,max,range)
    notnull
    number(min,max,range)
    (are there already spring annotations for thsi stuff)

