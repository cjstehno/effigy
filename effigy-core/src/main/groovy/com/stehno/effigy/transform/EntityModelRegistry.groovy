/*
 * Copyright (c) 2014 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
