
# Things to do

* Implement other associations
* Convert crud operation injection to be annotation-based so that you can pick and choose which you want

* convert the Column annotation to its own transformation - need to be careful since the property names need to be maintained
and we dont want other model properties to be modified by column annot or other applied annots
* consider a more layered approach to updating the model properties




    // @Ignored boolean active - TODO: add support for transient/ignored properties

    // TODO: support for component object
    // Occupation occupation (title, salary)

    /* FIXME: support
        onetoone - entity
        manytoone - entity (is this even valid for how my mapper works?)
        manytomany - collection, set, list, map
     */