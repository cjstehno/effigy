/*
 * Copyright (c) 2015 Christopher J. Stehno
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

package com.stehno.effigy.jdbc

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.Expression
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.SingleColumnRowMapper

import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics

/**
 * Registry of default RowMappers available by return value type. This class is intended for internal use.
 */
class RowMapperRegistry {

    // TODO: consider allowing custom global (or repo-scoped) registry of mappers by type

    private final Map<ClassNode, Expression> registry = prepareMapperRegistry()

    Expression findRowMapper(final ClassNode returnType) {
        registry.get(returnType) ?: ctorX(makeClassSafeWithGenerics(BeanPropertyRowMapper, returnType), args(constX(returnType.typeClass)))
    }

    private static Map<ClassNode, Expression> prepareMapperRegistry() {
        def mapperRegistry = [:]

        singleColumnRowMapperX(Byte_TYPE).with { m ->
            mapperRegistry.put byte_TYPE, m
            mapperRegistry.put Byte_TYPE, m
        }

        singleColumnRowMapperX(Character_TYPE).with { m ->
            mapperRegistry.put char_TYPE, m
            mapperRegistry.put Character_TYPE, m
        }

        singleColumnRowMapperX(Short_TYPE).with { m ->
            mapperRegistry.put short_TYPE, m
            mapperRegistry.put Short_TYPE, m
        }

        singleColumnRowMapperX(Integer_TYPE).with { m ->
            mapperRegistry.put int_TYPE, m
            mapperRegistry.put Integer_TYPE, m
        }

        singleColumnRowMapperX(Long_TYPE).with { m ->
            mapperRegistry.put long_TYPE, m
            mapperRegistry.put Long_TYPE, m
        }

        singleColumnRowMapperX(Float_TYPE).with { m ->
            mapperRegistry.put float_TYPE, m
            mapperRegistry.put Float_TYPE, m
        }

        singleColumnRowMapperX(Double_TYPE).with { m ->
            mapperRegistry.put double_TYPE, m
            mapperRegistry.put Double_TYPE, m
        }

        singleColumnRowMapperX(Boolean_TYPE).with { m ->
            mapperRegistry.put boolean_TYPE, m
            mapperRegistry.put Boolean_TYPE, m
        }

        mapperRegistry.put STRING_TYPE, singleColumnRowMapperX(STRING_TYPE)

        mapperRegistry
    }

    private static Expression singleColumnRowMapperX(ClassNode targetType) {
        // originally had an args block to specify the return type to the constructor but that has compilation issues
        ctorX(makeClassSafeWithGenerics(SingleColumnRowMapper, targetType))
    }
}
