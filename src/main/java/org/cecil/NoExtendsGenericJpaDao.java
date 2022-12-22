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

import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

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
  protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
    return new GenericJpaDaoApplicableTester();
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new GenericJpaDaoVisitor();
  }

  static class GenericJpaDaoApplicableTester extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.ClassDeclaration visitClassDeclaration(
        J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
      var visitedDeclaration = super.visitClassDeclaration(classDeclaration, ctx);
      if (!(visitedDeclaration.getType() instanceof JavaType.Class declaredType)
          || visitedDeclaration.getExtends() == null) {
        return visitedDeclaration;
      }

      if (extendsGenericJpaDao(declaredType)) {
        return SearchResult.found(visitedDeclaration, "Found a subtype of GenericJpaDao.");
      }
      return visitedDeclaration;
    }
  }

  static class GenericJpaDaoVisitor extends JavaIsoVisitor<ExecutionContext> {

    /**
     * Removes inheritance of GenericJpaDao from declaring class
     *
     * @param classDeclaration the declaring class
     * @param ctx the execution context
     * @return the updated class declaration
     */
    @Override
    public J.ClassDeclaration visitClassDeclaration(
        J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
      if (!(classDeclaration.getType() instanceof JavaType.Class declaredType)
          || classDeclaration.getExtends() == null) {
        return super.visitClassDeclaration(classDeclaration, ctx);
      }
      var result = classDeclaration;
      if (extendsGenericJpaDao(declaredType)) {
        result = updateClassDeclaration(classDeclaration, declaredType);
      }
      return super.visitClassDeclaration(result, ctx);
    }

    private J.ClassDeclaration updateClassDeclaration(
        final ClassDeclaration classDeclaration, final JavaType.Class declaredType) {
      var updatedType = getUpdatedType(declaredType);
      var result = getUpdatedClassDeclaration(classDeclaration, updatedType);
      maybeRemoveImport(FULLY_QUALIFIED_NAME);
      getCursor().putMessage(updatedType.getFullyQualifiedName(), updatedType);
      return result;
    }

    private ClassDeclaration getUpdatedClassDeclaration(
        final ClassDeclaration classDeclaration, final JavaType.Class updatedType) {
      return classDeclaration.withExtends(null).withType(updatedType);
    }

    private JavaType.Class getUpdatedType(final JavaType.Class declaredType) {
      return declaredType.withSupertype(null);
    }

    /**
     * Removes any overrides of the methods from GenericDao.
     *
     * @param methodDeclaration method declaration
     * @param context execution context
     * @return the updated method declaration
     */
    @Override
    public J.MethodDeclaration visitMethodDeclaration(
        J.MethodDeclaration methodDeclaration, ExecutionContext context) {
      final var methodType = methodDeclaration.getMethodType();
      if (methodType == null) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      var maybeClassType =
          getCursor().getNearestMessage(methodType.getDeclaringType().getFullyQualifiedName());
      if (!(maybeClassType instanceof JavaType.Class declaredType)) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      methodDeclaration =
          methodDeclaration.withMethodType(methodType.withDeclaringType(declaredType));
      if (methodDeclaration.getAllAnnotations().stream()
              .noneMatch(annotation -> isOfClassType(annotation.getType(), "java.lang.Override"))
          || TypeUtils.isOverride(methodType)) {
        return super.visitMethodDeclaration(methodDeclaration, context);
      }
      methodDeclaration =
          (J.MethodDeclaration)
              new RemoveAnnotation("@java.lang.Override")
                  .getVisitor()
                  .visitNonNull(methodDeclaration, context, getCursor().getParentOrThrow());
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
