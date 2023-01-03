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
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.JavaParser.fromJavaVersion;

import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.Block;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddEntityManagerToJpaDao extends Recipe {
  private static final String FULLY_QUALIFIED_NAME =
      "contrast.teamserver.dao.GenericJpaDao getEntityManager()";
  private static final MethodMatcher GET_ENTITY_MANAGER_MATCHER =
      new MethodMatcher(FULLY_QUALIFIED_NAME);

  @Override
  public String getDisplayName() {
    return "Add `EntityManager` to subclasses of GenericJpaDao";
  }

  @Override
  public String getDescription() {
    return """
        Replaces usages of `GenericDao#getEntityManager()`
        with an `EntityManager` instance variable.""";
  }

  //  @Override
  //  protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
  //    return new CallsEntityManagerApplicableTester();
  //  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new AddEntityManagerVisitor();
  }

  private static class AddEntityManagerVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static boolean done;
    private final JavaTemplate addEntityManager =
        JavaTemplate.builder(
                this::getCursor,
                """
              private EntityManager entityManager;
              """)
            .javaParser(() -> fromJavaVersion().classpath("main").build())
            .imports(
                "lombok.Setter",
                "lombok.AccessLevel",
                "javax.persistence.PersistenceContext",
                "javax.persistence.EntityManager")
            .build();
    private final JavaTemplate replaceMethodCall =
        JavaTemplate.builder(this::getCursor, "entityManager").build();

    private J.Modifier privateModifier() {
      return new J.Modifier(
          Tree.randomId(),
          Space.build(" ", emptyList()),
          Markers.EMPTY,
          Type.Private,
          Collections.emptyList());
    }

//    @Override
//    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
//      J.Block b = super.visitBlock(block, ctx);
//      List<Statement> allStatements =
//          ListUtils.flatMap(
//              b.getStatements(),
//              rawStatement -> {
//                if (!(rawStatement instanceof VariableDeclarations statement)
//                    || getCursor().firstEnclosing(ClassDeclaration.class) == null
//                    || done) {
//                  return rawStatement;
//                }
//                var coordinates = statement.getCoordinates().after();
//                J.VariableDeclarations entityManagerDeclaration =
//                    autoFormat(
//                        statement
//                            .withId(randomId())
//                            .withVariables(
//                                ListUtils.map(
//                                    statement.getVariables(),
//                                    v ->
//                                        v.withInitializer(null)
//                                            .withVariableType(
//                                                new JavaType.Variable(
//                                                    null,
//                                                    Flag.Private.getBitMask(),
//                                                    v.getSimpleName(),
//                                                    getCursor()
//                                                        .firstEnclosingOrThrow(
//                                                            J.ClassDeclaration.class)
//                                                        .getType(),
//                                                    v.getType(),
//                                                    emptyList()))))
//                            .withModifiers(singletonList(privateModifier())),
//                        ctx,
//                        getCursor());
//                entityManagerDeclaration =
//                    entityManagerDeclaration.withTemplate(addEntityManager, coordinates);
//                maybeAddImport("lombok.Setter");
//                maybeAddImport("lombok.AccessLevel");
//                maybeAddImport("javax.persistence.PersistenceContext");
//                maybeAddImport("javax.persistence.EntityManager");
//                done = true;
//                return entityManagerDeclaration;
//              });
//
//      return b.withStatements(allStatements);
//    }


    @Override
    public Block visitBlock(Block block,
        ExecutionContext executionContext) {
      var visited = super.visitBlock(block, executionContext);

      if (getCursor().firstEnclosing(ClassDeclaration.class) == null) {
        return visited;
      }

      VariableDeclarations addEntityManagerDeclaration = new VariableDeclarations(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), List.of(privateModifier()), , , , )

    }
  }

  private Annotation setterAnnotation() {
    return new Annotation(randomId() ,Space.EMPTY, Markers.EMPTY,);
  }

}

    //            @Override
    //            public VariableDeclarations visitVariableDeclarations(
    //                final VariableDeclarations multiVariable, final ExecutionContext
    // executionContext)
    //     {
    //              // if we're here it means there's at least one variable declaration in the
    // class.
    //              // now we check if this variable is static,
    //              // (since static variables should go first, we skip this visit)
    //              if (multiVariable.hasModifier(Type.Static)) {
    //                return super.visitVariableDeclarations(multiVariable, executionContext);
    //              }
    //
    //              var enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
    //              // Skip this visit as it's of no interest to us
    //              if (enclosingClass == null) {
    //                return super.visitVariableDeclarations(multiVariable, executionContext);
    //              }
    //
    //              var enclosingMethod = getCursor().firstEnclosing(MethodDeclaration.class);
    //              // We've made it to a method, which means there are no instance variable
    //              // We'll use this as our insertion point.
    //              JavaCoordinates coordinates;
    //              if (enclosingMethod != null) {
    //                coordinates = enclosingMethod.getCoordinates().before();
    //              } else {
    //                coordinates = multiVariable.getCoordinates().after();
    //              }
    //       final VariableDeclarations result = multiVariable.withTemplate(addEntityManager,
    //           coordinates);
    //                return super.visitVariableDeclarations(result, executionContext);
    ////              return super.visitVariableDeclarations(multiVariable, executionContext);
    //            }

    //    @Override
    //    public MethodInvocation visitMethodInvocation(
    //        final MethodInvocation method, final ExecutionContext executionContext) {
    //
    //      if (GET_ENTITY_MANAGER_MATCHER.matches(method)) {
    //        var enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
    //        var enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
    ////        var firstInstanceVariableDeclaration =
    ////            enclosingClass.getBody().getStatements().stream().filter(it -> it.isScope());
    //            maybeAddImport("lombok.Setter");
    //            maybeAddImport("lombok.AccessLevel");
    //            maybeAddImport("javax.persistence.PersistenceContext");
    //            maybeAddImport("javax.persistence.EntityManager");

    //        return method.withTemplate(addEntityManager, method.getCoordinates().replace());
    //      }
    //
    //      return super.visitMethodInvocation(method, executionContext);
    //    }
    //  }

    //  private static class CallsEntityManagerApplicableTester extends
    // JavaIsoVisitor<ExecutionContext> {
    //    @Override
    //    public MethodInvocation visitMethodInvocation(
    //        final MethodInvocation method, final ExecutionContext executionContext) {
    //      doAfterVisit(new UsesMethod<>(GET_ENTITY_MANAGER_MATCHER));
    //      return method;
    //    }
    //  }
  }
}
