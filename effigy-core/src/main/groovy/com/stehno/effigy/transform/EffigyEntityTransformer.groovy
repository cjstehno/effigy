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

import static com.stehno.effigy.logging.Logger.info
import static com.stehno.effigy.transform.AstUtils.codeS
import static com.stehno.effigy.transform.EntityModel.registerEntityModel
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafe
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

import com.stehno.effigy.jdbc.EffigyAssociationResultSetExtractor
import com.stehno.effigy.jdbc.EffigyCollectionAssociationResultSetExtractor
import com.stehno.effigy.jdbc.EffigyEntityRowMapper
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
 * Handles the transformation of the Effigy Entity classes (annotated with the EffigyEntity annotation).
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class EffigyEntityTransformer implements ASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode entityClassNode = nodes[1] as ClassNode

        verifyVersionProperty(entityClassNode)

        EntityModel model = registerEntityModel(entityClassNode)

        ClassNode mapperClassNode = buildRowMapper(model, source)
        injectRowMapperAccessor(entityClassNode, mapperClassNode, model)

        if (model.hasAssociations()) {
            ClassNode extractorClassNode = buildAssociationExtractor(model, source)
            injectExtractorAccessor(entityClassNode, extractorClassNode, model)

            ClassNode collectionExtractorClassNode = buildCollectionAssociationExtractor(model, source)
            injectCollectionExtractorAccessor(entityClassNode, collectionExtractorClassNode, model)
        }
    }

    private static void verifyVersionProperty(final ClassNode entityClassNode) {
        FieldNode versionProperty = entityClassNode.fields.find { FieldNode f ->
            f.annotations.find { AnnotationNode a -> a.classNode.name == 'com.stehno.effigy.annotation.Version' }
        }

        if (versionProperty && !(versionProperty.type in [Long_TYPE, long_TYPE])) {
            throw new Exception('Currently the Version annotation may only be used on long or java.lang.Long fields.')
        }
    }

    /**
     * Creates a RowMapper implementation class for each annotated entity.
     *
     * @param model the entity model
     * @param source the source unit
     * @return the created RowMapper implementation class
     */
    private static ClassNode buildRowMapper(EntityModel model, SourceUnit source) {
        ClassNode mapperClassNode = null
        try {
            mapperClassNode = new ClassNode(
                "${model.type.packageName}.${model.type.nameWithoutPackage}RowMapper",
                Modifier.PUBLIC,
                makeClassSafe(EffigyEntityRowMapper),
                [] as ClassNode[],
                [] as MixinNode[]
            )

            mapperClassNode.addMethod(new MethodNode(
                'newEntity',
                Modifier.PROTECTED,
                newClass(model.type),
                [] as Parameter[],
                [] as ClassNode[],
                new ReturnStatement(ctorX(newClass(model.type)))
            ))

            mapperClassNode.addMethod(new MethodNode(
                'mapping',
                Modifier.PROTECTED,
                newClass(model.type),
                [param(make(ResultSet), 'rs'), param(OBJECT_TYPE, 'entity')] as Parameter[],
                [] as ClassNode[],
                block(
                    codeS(
                        '''
                        <% model.findProperties().each { p-> %>
                            entity.${p.propertyName} = rs.getObject( prefix + '${p.columnName}' )
                        <% } %>
                        return ( entity.${model.identifier.propertyName} ? entity : null )
                        ''',
                        model: model
                    )
                )
            ))

            source.AST.addClass(mapperClassNode)

            info EffigyEntityTransformer, 'Injected row mapper ({}) for {}', mapperClassNode.name, model.type

        } catch (ex) {
            ex.printStackTrace()
        }
        mapperClassNode
    }

    /**
     * Injects a helper method into the entity. This helper method allows for simple retrieval of the generated row mapper with
     * optional prefix definition. The method signature is:
     *
     * public static rowMapper(String prefix='')
     *
     * @param entityClassNode
     * @param mapperClassNode
     * @param model
     */
    private static void injectRowMapperAccessor(ClassNode entityClassNode, ClassNode mapperClassNode, EntityModel model) {
        entityClassNode.addMethod(new MethodNode(
            'rowMapper',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(mapperClassNode),
            [new Parameter(STRING_TYPE, 'prefix', constX(''))] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(mapperClassNode), args(new MapExpression([new MapEntryExpression(constX('prefix'), varX('prefix'))]))))
        ))

        info EffigyEntityTransformer, 'Injected row mapper helper method for {}', model.type
    }

    /**
     * Adds an implementation of the EffigyAssociationResultSetExtractor for the entity.
     *
     * @param model
     * @param source
     * @return
     */
    private static ClassNode buildAssociationExtractor(EntityModel model, SourceUnit source) {
        ClassNode classNode = null
        try {
            classNode = new ClassNode(
                "${model.type.packageName}.${model.type.nameWithoutPackage}AssociationExtractor",
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
                new ReturnStatement(callX(classX(newClass(model.type)), 'rowMapper', args(constX("${model.table}_" as String))))
            ))

            model.findAssociationProperties().each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
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
                    <% model.findAssociationProperties().each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} << ${ap.propertyName}Value
                        }
                    <% } %>
                    ''',
                    model: model
                )
            ))

            source.AST.addClass(classNode)

        } catch (ex) {
            ex.printStackTrace()
        }
        classNode
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
    private static void injectExtractorAccessor(ClassNode entityClassNode, ClassNode extractorClassNode, EntityModel model) {
        model.findAssociationProperties()

        entityClassNode.addMethod(new MethodNode(
            'associationExtractor',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(extractorClassNode),
            [] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(extractorClassNode)))
        ))

        info EffigyEntityTransformer, 'Injected association extractor helper method for {}', model.type
    }

    /**
     * Adds an implementation of the EffigyAssociationResultSetExtractor for the entity.
     *
     * @param model
     * @param source
     * @return
     */
    private static ClassNode buildCollectionAssociationExtractor(EntityModel model, SourceUnit source) {
        ClassNode classNode = null
        try {
            classNode = new ClassNode(
                "${model.type.packageName}.${model.type.nameWithoutPackage}CollectionAssociationExtractor",
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
                new ReturnStatement(callX(classX(newClass(model.type)), 'rowMapper', args(constX("${model.table}_" as String))))
            ))

            model.findAssociationProperties().each { ap ->
                classNode.addMethod(new MethodNode(
                    "${ap.propertyName}RowMapper",
                    Modifier.PROTECTED,
                    makeClassSafe(RowMapper),
                    [] as Parameter[],
                    [] as ClassNode[],
                    new ReturnStatement(callX(classX(newClass(ap.associatedType)), 'rowMapper', args(constX("${ap.propertyName}_" as String))))
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
                    <% model.findAssociationProperties().each { ap-> %>
                        def ${ap.propertyName}Value = ${ap.propertyName}RowMapper().mapRow(rs,0)
                        if( ${ap.propertyName}Value ){
                            entity.${ap.propertyName} << ${ap.propertyName}Value
                        }
                    <% } %>
                    ''',
                    model: model
                )
            ))

            source.AST.addClass(classNode)

        } catch (ex) {
            ex.printStackTrace()
        }
        classNode
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
    private static void injectCollectionExtractorAccessor(ClassNode entityClassNode, ClassNode extractorClassNode, EntityModel model) {
        model.findAssociationProperties()

        entityClassNode.addMethod(new MethodNode(
            'collectionAssociationExtractor',
            Modifier.PUBLIC | Modifier.STATIC,
            newClass(extractorClassNode),
            [] as Parameter[],
            [] as ClassNode[],
            returnS(ctorX(newClass(extractorClassNode), args(new MapExpression([
                new MapEntryExpression(constX('entityIdentifier'), constX(model.identifier.propertyName))
            ]))))
        ))

        info EffigyEntityTransformer, 'Injected collection association extractor helper method for {}', model.type
    }
}
