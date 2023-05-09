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

import static org.openrewrite.java.tree.JavaType.buildType;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

@Value
@EqualsAndHashCode(callSuper = true)
public class NoExtendsGenericJpaDao extends Recipe {

  private static final String FULLY_QUALIFIED_NAME = "contrast.teamserver.dao.GenericJpaDao";
  private static final TypeMatcher GENERIC_JPA_DAO = new TypeMatcher(FULLY_QUALIFIED_NAME);

  private static boolean extendsGenericJpaDao(final JavaType.Class declaredType) {
    //Maybe GENERIC_JPA_DAO.matches(declaredType.getSupertype()); ?
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
        Objects.requireNonNull(TypeUtils.asFullyQualified(buildType("contrast.teamserver.dao.GenericJpaDao")));

    /**
     * Removes inheritance of GenericJpaDao from declaring class
     *
     * @param classDecl the declaring class
     * @param context   the execution context
     * @return the updated class declaration
     */
    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext context) {

      if (TypeUtils.isAssignableTo(deprecatedImplType, classDecl.getType())) {
        maybeRemoveImport(FULLY_QUALIFIED_NAME);
        // Remove extends clause.
        classDecl = classDecl.withExtends(null);
        var annotations = List.of(
            new J.Annotation(UUID.randomUUID(), Space.EMPTY, Markers.build(List.of(IntelliJ.defaults())),
                             new Identifier(UUID.randomUUID(), Space.EMPTY, Markers.build(List.of(IntelliJ.defaults())),
                                            "javax.persistence.PersistenceContext",
                                            buildType("javax.persistence.PersistenceContext"), null), null));
        var modifiers = List.of(
            new Modifier(UUID.randomUUID(), Space.EMPTY, Markers.build(List.of(IntelliJ.defaults())), Type.Private,
                         annotations));
        var entityManagerField =
            new J.VariableDeclarations(UUID.randomUUID(), Space.EMPTY, Markers.build(List.of(IntelliJ.defaults())),
                                       annotations, modifiers, null, null, List.of(JLeftPadded.build(Space.EMPTY)),
                                       List.of());
        var ogStatements = classDecl.getBody().getStatements();
        List<Statement> newStatements = new ArrayList<>(ogStatements);
        newStatements.add(0, entityManagerField);
        classDecl = classDecl.withBody(classDecl.getBody().withStatements(newStatements));
      }
      return super.visitClassDeclaration(classDecl, context);
    }

    /**
     * Removes any overrides of the methods from GenericDao.
     *
     * @param methodDeclaration method declaration
     * @param context           execution context
     * @return the updated method declaration
     */
    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext context) {
      final var methodType = methodDeclaration.getMethodType();
      if (methodType == null) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      var maybeClassType = getCursor().getNearestMessage(methodType.getDeclaringType().getFullyQualifiedName());
      if (!(maybeClassType instanceof JavaType.Class declaredType)) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      methodDeclaration = methodDeclaration.withMethodType(methodType.withDeclaringType(declaredType));
      if (methodDeclaration.getAllAnnotations().stream()
                           .noneMatch(annotation -> isOfClassType(annotation.getType(), "java.lang.Override")) ||
          TypeUtils.isOverride(methodType)) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      methodDeclaration = (J.MethodDeclaration) new RemoveAnnotation("@java.lang.Override").getVisitor().visitNonNull(
          methodDeclaration, context, getCursor().getParentOrThrow());
      return super.visitMethodDeclaration(methodDeclaration, context);
    }

    @Override
    public MethodInvocation visitMethodInvocation(final MethodInvocation invocation,
                                                  final ExecutionContext executionContext) {
      invocation.getMethodType();

      return super.visitMethodInvocation(invocation, executionContext);
    }
  }
}
