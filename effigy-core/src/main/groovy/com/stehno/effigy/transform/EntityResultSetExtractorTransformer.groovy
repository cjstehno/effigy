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
import static com.stehno.effigy.logging.Logger.error
import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.model.EntityModel.*
import static com.stehno.effigy.transform.util.AstUtils.codeS
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.ClassHelper.VOID_TYPE
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

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
/**
 * Transformer used for creating a ResultSetExtractor instance for the entity.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EntityResultSetExtractorTransformer implements ASTTransformation {

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
                'primaryRowMapper',
                Modifier.PROTECTED,
                makeClassSafe(RowMapper),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(callX(classX(newClass(entityNode)), 'rowMapper', args(constX("${entityTable(entityNode)}_" as String))))
            ))

            oneToManyAssociations(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
                ))
            }

            components(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.type)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
                ))
            }

            classNode.addMethod(new MethodNode(
                'mapAssociations',
                Modifier.PROTECTED,
                VOID_TYPE,
                [param(makeClassSafe(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[],
                [] as ClassNode[],
                codeS(
                    '''
                    <% oneToManys.each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} << ${ap.propertyName}Value
                        }
                    <% } %>
                    <% oneToOnes.each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} = ${ap.propertyName}Value
                        }
                    <% } %>
                    ''',
                    oneToManys: oneToManyAssociations(entityNode),
                    oneToOnes: components(entityNode)
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

        info EntityTransformer, 'Injected association extractor helper method for {}', entityClassNode.name
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
                'primaryRowMapper',
                Modifier.PROTECTED,
                makeClassSafe(RowMapper),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(callX(classX(newClass(entityNode)), 'rowMapper', args(constX("${entityTable(entityNode)}_" as String))))
            ))

            oneToManyAssociations(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
                ))
            }

            components(entityNode).each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.type)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
                ))
            }

            classNode.addMethod(new MethodNode(
                'mapAssociations',
                Modifier.PROTECTED,
                VOID_TYPE,
                [param(makeClassSafe(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[],
                [] as ClassNode[],
                codeS(
                    '''
                    <% oneToManys.each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} << ${ap.propertyName}Value
                        }
                    <% } %>
                    <% oneToOnes.each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} = ${ap.propertyName}Value
                        }
                    <% } %>
                    ''',
                    oneToManys: oneToManyAssociations(entityNode),
                    oneToOnes: components(entityNode)
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
    private static void injectCollectionExtractorAccessor(ClassNode entityClassNode, ClassNode extractorClassNode) {
        entityClassNode.addMethod(new MethodNode(
            'collectionAssociationExtractor',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(extractorClassNode),
            [] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(extractorClassNode), args(new MapExpression([
                new MapEntryExpression(constX('entityIdentifier'), constX(identifier(entityClassNode).propertyName))
            ]))))
        ))

        info EntityTransformer, 'Injected collection association extractor helper method for {}', entityClassNode.name
    }
}
