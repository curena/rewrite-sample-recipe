/*-
 * #%L
 * Contrast TeamServer
 * %%
 * Copyright (C) 2022 Contrast Security, Inc.
 * %%
 * Contact: support@contrastsecurity.com
 * License: Commercial
 * NOTICE: This Software and the patented inventions embodied within may only be
 * used as part of Contrast Security’s commercial offerings. Even though it is
 * made available through public repositories, use of this Software is subject to
 * the applicable End User Licensing Agreement found at
 * https://www.contrastsecurity.com/enduser-terms-0317a or as otherwise agreed
 * between Contrast Security and the End User. The Software may not be reverse
 * engineered, modified, repackaged, sold, redistributed or otherwise used in a
 * way not consistent with the End User License Agreement.
 * #L%
 */
package org.cecil;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.JavaType.buildType;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.ClassDeclaration.Kind;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.J.TypeParameter;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Class;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Space.Location;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

@Value
@EqualsAndHashCode(callSuper = true)
public class NoExtendsGenericJpaDao extends Recipe {

  private static final String FULLY_QUALIFIED_NAME = "contrast.teamserver.dao.GenericJpaDao";
  private static final TypeMatcher GENERIC_JPA_DAO = new TypeMatcher(FULLY_QUALIFIED_NAME);

  private static boolean extendsGenericJpaDao(final JavaType.Class declaredType) {
    // Maybe GENERIC_JPA_DAO.matches(declaredType.getSupertype()); ?
    return isOfClassType(declaredType.getSupertype(), FULLY_QUALIFIED_NAME);
  }

  @Override
  public String getDisplayName() {
    return "Do not extend GenericJpaDao";
  }

  @Override
  public String getDescription() {
    return "Removes inheritance of deprecated class GenericJpaDao.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new GenericJpaDaoVisitor();
  }

  static class GenericJpaDaoVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final JavaType.FullyQualified deprecatedImplType =
        Objects.requireNonNull(TypeUtils.asFullyQualified(buildType(FULLY_QUALIFIED_NAME)));

    /**
     * Removes inheritance of GenericJpaDao from declaring class
     *
     * @param classDecl the declaring class
     * @param context the execution context
     * @return the updated class declaration
     */
    @Override
    public J.ClassDeclaration visitClassDeclaration(
        J.ClassDeclaration classDecl, ExecutionContext context) {

      if (TypeUtils.isAssignableTo(deprecatedImplType, classDecl.getType())) {
        maybeRemoveImport(FULLY_QUALIFIED_NAME);
        // Remove extends clause.
        classDecl = classDecl.withExtends(null);
        classDecl = addEntityManagerField(classDecl);
      }
      return super.visitClassDeclaration(classDecl, context);
    }

    private J.ClassDeclaration addEntityManagerField(J.ClassDeclaration classDeclaration) {
      // get a JavaType.Class corresponding to EntityManager
      // check if classDeclaration already has an EntityManager field
      if (shouldAddEntityManager(classDeclaration)) {
        var entityManagerType = buildType("jakarta.persistence.EntityManager");
        var entityManagerIdentifier =
            new J.Identifier(
                randomId(),
                Space.EMPTY,
                Markers.build(List.of(IntelliJ.defaults())),
                "entityManager",
                entityManagerType,
                null);
        // Create a variable with name "varName" and type fieldType
        var entityManagerVariable =
            new J.VariableDeclarations.NamedVariable(
                randomId(), EMPTY, Markers.EMPTY, entityManagerIdentifier, emptyList(), null, null);
        var annotations =
            List.of(
                new J.Annotation(
                    UUID.randomUUID(),
                    Space.build("\n", Collections.emptyList()),
                    Markers.build(List.of(IntelliJ.defaults())),
                    new Identifier(
                        UUID.randomUUID(),
                        Space.EMPTY,
                        Markers.build(List.of(IntelliJ.defaults())),
                        "PersistenceContext",
                        null,
                        null),
                    null));
        var modifiers =
            List.of(
                new Modifier(
                    UUID.randomUUID(),
                    Space.build("\n", Collections.emptyList()),
                    Markers.build(List.of(IntelliJ.defaults())),
                    Type.Private,
                    Collections.emptyList()));
        var rightPadded = JRightPadded.build(entityManagerVariable);
        J.VariableDeclarations declarations =
            new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                annotations,
                modifiers,
                new J.Identifier(
                    randomId(),
                    Space.build(" ", Collections.emptyList()),
                    Markers.EMPTY,
                    "EntityManager",
                    JavaType.ShallowClass.build("jakarta.persistence.EntityManager"),
                    null),
                null,
                Collections.emptyList(),
                List.of(rightPadded));
        var ogStatements = classDeclaration.getBody().getStatements();
        List<Statement> newStatements = new ArrayList<>(ogStatements);
        newStatements.add(0, declarations);
        var newClassDec = copyClassDeclaration(classDeclaration);
        newClassDec = newClassDec.withBody(classDeclaration.getBody().withStatements(newStatements));
        return newClassDec;
      }
      return classDeclaration;
    }

    private ClassDeclaration copyClassDeclaration(final ClassDeclaration classDeclaration) {
      var typeParameters = Optional.ofNullable(classDeclaration.getTypeParameters())params ->
          params.stream()
              .map(i -> new JRightPadded<>(i, EMPTY, Markers.EMPTY))
              .toList());

      var primaryConstructor = Optional.ofNullable(classDeclaration.getPrimaryConstructor());
      return new ClassDeclaration(
          classDeclaration.getId(),
          classDeclaration.getPrefix(),
          classDeclaration.getMarkers(),
          classDeclaration.getAnnotations().getKind().getAnnotations(),
          classDeclaration.getModifiers(),
          new Kind(
              randomId(),
              EMPTY,
              Markers.EMPTY,
              Collections.emptyList(),
              classDeclaration.getKind()),
          classDeclaration.getName(),
          typeParameters,
          JContainer.build(
              Objects.requireNonNull(classDeclaration.getPrimaryConstructor()).stream()
                  .map(i -> new JRightPadded<Statement>(i, EMPTY, Markers.EMPTY))
                  .toList()),
          new JLeftPadded<>(
              EMPTY, Objects.requireNonNull(classDeclaration.getExtends()), Markers.EMPTY),
          JContainer.build(
              classDeclaration.getImplements().stream()
                  .map(i -> new JRightPadded<TypeTree>(i, EMPTY, Markers.EMPTY))
                  .toList()),
          JContainer.build(
              classDeclaration.getPermits().stream()
                  .map(i -> new JRightPadded<TypeTree>(i, EMPTY, Markers.EMPTY))
                  .toList()),
          classDeclaration.getBody(),
          null);
    }

    private boolean shouldAddEntityManager(ClassDeclaration classDeclaration) {
      var entityManagerType = buildType("jakarta.persistence.EntityManager");
      return classDeclaration.getBody().getStatements().stream()
          .noneMatch(
              statement ->
                  statement instanceof J.VariableDeclarations decs
                      && decs.getVariables().stream()
                          .anyMatch(
                              variable -> Objects.equals(variable.getType(), entityManagerType)));
    }
  }

  //    @Override
  //    public J.MethodDeclaration visitMethodDeclaration(
  //        J.MethodDeclaration methodDeclaration, ExecutionContext context) {
  //      final var methodType = methodDeclaration.getMethodType();
  //      if (methodType == null) {
  //        return super.visitMethodDeclaration(methodDeclaration, context);
  //      }
  //      var maybeClassType =
  //          getCursor().getNearestMessage(methodType.getDeclaringType().getFullyQualifiedName());
  //      if (!(maybeClassType instanceof JavaType.Class declaredType)) {
  //        return super.visitMethodDeclaration(methodDeclaration, context);
  //      }
  //      methodDeclaration =
  //          methodDeclaration.withMethodType(methodType.withDeclaringType(declaredType));
  //      if (methodDeclaration.getAllAnnotations().stream()
  //              .noneMatch(annotation -> isOfClassType(annotation.getType(),
  // "java.lang.Override"))
  //          || TypeUtils.isOverride(methodType)) {
  //        return super.visitMethodDeclaration(methodDeclaration, context);
  //      }
  //      methodDeclaration =
  //          (J.MethodDeclaration)
  //              new RemoveAnnotation("@java.lang.Override")
  //                  .getVisitor()
  //                  .visitNonNull(methodDeclaration, context, getCursor().getParentOrThrow());
  //      return super.visitMethodDeclaration(methodDeclaration, context);
  //    }

  //    @Override
  //    public MethodInvocation visitMethodInvocation(
  //        final MethodInvocation invocation, final ExecutionContext executionContext) {
  //      invocation.getMethodType();
  //
  //      return super.visitMethodInvocation(invocation, executionContext);
  //    }
}
