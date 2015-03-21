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

package com.stehno.effigy.transform

import com.stehno.effigy.jdbc.EffigyAssociationResultSetExtractor
import com.stehno.effigy.jdbc.EffigyCollectionAssociationResultSetExtractor
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.jdbc.core.RowMapper

import java.lang.reflect.Modifier
import java.sql.ResultSet

import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

/**
 * Transformer used for creating a <code>ResultSetExtractor</code> instance for the entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@SuppressWarnings('GStringExpressionWithinString')
class EntityResultSetExtractorTransformer implements ASTTransformation {

    private static final String ROW_MAPPER = 'rowMapper'
    private static final String MAP_ASSOCIATIONS = 'mapAssociations'
    private static final String RS = 'rs'
    private static final String PRIMARY_ROW_MAPPER = 'primaryRowMapper'
    private static final String ENTITY = 'entity'
    private static final String LIMIT = 'limit'
    private static final String OFFSET = 'offset'

    // FIXME: it would be nice if this code was a bit more baked in - this generates "bad" code for maps
    // - groovy doesn't care since its never executed, but need to clean it up
    private static final String ASSOCIATION_EXTRACTION_SOURCE = '''
        <% assocs.each { ap-> %>
            def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
            if( ${ap.propertyName}Value ){
                if( entity.${ap.propertyName} instanceof Collection ){
                    entity.${ap.propertyName} << ${ap.propertyName}Value
                } else if( entity.${ap.propertyName} instanceof Map ){
                    entity.${ap.propertyName}[${ap.propertyName}Value.${ap.mapKeyProperty}] = ${ap.propertyName}Value
                } else {
                    entity.${ap.propertyName} = ${ap.propertyName}Value
                }
            }
        <% } %>
        <% compos.each { ap-> %>
            def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
            if( ${ap.propertyName}Value ){
                entity.${ap.propertyName} = ${ap.propertyName}Value
            }
        <% } %>
    '''

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode

        if (hasAssociatedEntities(entityClassNode)) {
            info EntityResultSetExtractorTransformer, 'Creating ResultSetExtractor for: {}', entityClassNode.name

            ClassNode extractorClassNode = buildAssociationExtractor(entityClassNode, source)
            injectExtractorAccessor(entityClassNode, extractorClassNode)

            ClassNode collectionExtractorClassNode = buildCollectionAssociationExtractor(entityClassNode, source)
            injectCollectionExtractorAccessor(entityClassNode, collectionExtractorClassNode)
        }
    }

    /**
     * Adds an implementation of the EffigyAssociationResultSetExtractor for the entity.
     *
     * @param model
     * @param source
     * @return
     */
    private static ClassNode buildAssociationExtractor(ClassNode entityNode, SourceUnit source) {
        String extractorName = "${entityNode.packageName}.${entityNode.nameWithoutPackage}AssociationExtractor"
        try {
            ClassNode classNode = new ClassNode(
                extractorName,
                Modifier.PUBLIC,
                makeClassSafe(EffigyAssociationResultSetExtractor),
                [] as ClassNode[],
                [] as MixinNode[]
            )

            classNode.addMethod(new MethodNode(
                PRIMARY_ROW_MAPPER,
                Modifier.PROTECTED,
                makeClassSafe(RowMapper),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(callX(classX(newClass(entityNode)), ROW_MAPPER, args(constX("${entityTable(entityNode)}_" as String))))
            ))

            associations(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), ROW_MAPPER, args(constX("${ap.propertyName}_" as String))))
                ))
            }

            components(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.type)), ROW_MAPPER, args(constX("${ap.propertyName}_" as String))))
                ))
            }

            classNode.addMethod(new MethodNode(
                MAP_ASSOCIATIONS,
                Modifier.PROTECTED,
                VOID_TYPE,
                [param(makeClassSafe(ResultSet), RS), param(OBJECT_TYPE, ENTITY)] as Parameter[],
                [] as ClassNode[],
                codeS(
                    ASSOCIATION_EXTRACTION_SOURCE,
                    assocs: associations(entityNode),
                    compos: components(entityNode)
                )
            ))

            source.AST.addClass(classNode)

            return classNode

        } catch (ex) {
            error EntityResultSetExtractorTransformer, 'Problem building ResultSetExtractor ({}): {}', extractorName, ex.message
            throw ex
        }
    }

    /**
     * Provides a static accessor method for the ResultSetExtractor. The method signature will be:
     *
     * public static associationExtractor()
     *
     * @param entityClassNode
     * @param extractorClassNode
     * @param model
     */
    private static void injectExtractorAccessor(ClassNode entityClassNode, ClassNode extractorClassNode) {
        entityClassNode.addMethod(new MethodNode(
            'associationExtractor',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(extractorClassNode),
            [] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(extractorClassNode)))
        ))

        info getClass(), 'Injected association extractor helper method for {}', entityClassNode.name
    }

    /**
     * Adds an implementation of the EffigyAssociationResultSetExtractor for the entity.
     *
     * @param model
     * @param source
     * @return
     */
    private static ClassNode buildCollectionAssociationExtractor(ClassNode entityNode, SourceUnit source) {
        def extractorName = "${entityNode.packageName}.${entityNode.nameWithoutPackage}CollectionAssociationExtractor"
        try {
            ClassNode classNode = new ClassNode(
                extractorName,
                Modifier.PUBLIC,
                makeClassSafe(EffigyCollectionAssociationResultSetExtractor),
                [] as ClassNode[],
                [] as MixinNode[]
            )

            classNode.addMethod(new MethodNode(
                PRIMARY_ROW_MAPPER,
                Modifier.PROTECTED,
                makeClassSafe(RowMapper),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(callX(classX(newClass(entityNode)), ROW_MAPPER, args(constX("${entityTable(entityNode)}_" as String))))
            ))

            associations(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), ROW_MAPPER, args(constX("${ap.propertyName}_" as String))))
                ))
            }

            components(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.type)), ROW_MAPPER, args(constX("${ap.propertyName}_" as String))))
                ))
            }

            classNode.addMethod(new MethodNode(
                MAP_ASSOCIATIONS,
                Modifier.PROTECTED,
                VOID_TYPE,
                [param(makeClassSafe(ResultSet), RS), param(OBJECT_TYPE, ENTITY)] as Parameter[],
                [] as ClassNode[],
                codeS(
                    ASSOCIATION_EXTRACTION_SOURCE,
                    assocs: associations(entityNode),
                    compos: components(entityNode)
                )
            ))

            source.AST.addClass(classNode)

            return classNode

        } catch (ex) {
            error EntityResultSetExtractorTransformer, 'Problem building collection ResultSetExtractor ({}): {}', extractorName, ex.message
            throw ex
        }
    }

    /**
     * Provides a static accessor method for the ResultSetExtractor. The method signature will be:
     *
     * public static associationExtractor()
     *
     * @param entityClassNode
     * @param extractorClassNode
     * @param model
     */
    private static void injectCollectionExtractorAccessor(ClassNode entityClassNode, ClassNode extractorClassNode) {
        entityClassNode.addMethod(new MethodNode(
            'collectionAssociationExtractor',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(extractorClassNode),
            params(param(Integer_TYPE, OFFSET, constX(null)), param(Integer_TYPE, LIMIT, constX(null))),
            [] as ClassNode[],
            returnS(ctorX(newClass(extractorClassNode), args(new MapExpression([
                new MapEntryExpression(constX('entityIdentifier'), constX(identifier(entityClassNode).propertyName)),
                new MapEntryExpression(constX(OFFSET), varX(OFFSET)),
                new MapEntryExpression(constX(LIMIT), varX(LIMIT))
            ]))))
        ))

        info getClass(), 'Injected collection association extractor helper method for {}', entityClassNode.name
    }
}
