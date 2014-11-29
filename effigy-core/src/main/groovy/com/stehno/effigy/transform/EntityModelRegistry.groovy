package com.stehno.effigy.transform

import org.codehaus.groovy.ast.ClassNode

/**
 * Created by cjstehno on 11/29/2014.
 */
@Singleton
class EntityModelRegistry {

    private final Map<ClassNode, EntityModel> entityModels = [:]

    EntityModel register(EntityModel model) {
        entityModels[model.type] = model
        model
    }

    EntityModel lookup(ClassNode entityType) {
        entityModels[entityType]
    }
}
