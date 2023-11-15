package org.cecil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

public class NoExtendsGenericJpaDao extends Recipe {

  @Override
  public String getDisplayName() {
    return "Add EntityManager to GenericDao Implementations";
  }

  @Override
  public String getDescription() {
    return "Adds EntityManager to GenericDao Implementations.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new GenericJpaDaoVisitor();
  }

  static class GenericJpaDaoVisitor extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public J.ClassDeclaration visitClassDeclaration(
        J.ClassDeclaration classDecl, ExecutionContext ctx) {
      if (implementsInterface(classDecl) && (!entityManagerExists(classDecl))) {
        J.VariableDeclarations entityManagerField = createEntityManagerField();
        // Get the existing body of the class
        J.Block body = classDecl.getBody();
        // Create a new list of statements for the body that includes the new variable
        List<Statement> statements = new ArrayList<>(body.getStatements());
        statements.add(0, entityManagerField); // Adding at the beginning of the class body

        // Create a new body with the updated statements

        var name =
            new JLeftPadded<>(
                Space.EMPTY,
                new J.Identifier(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    Collections.emptyList(),
                    "entityManager",
                    null,
                    null),
                Markers.EMPTY);

        var target =
            new J.Identifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Collections.emptyList(),
                "this",
                null,
                null);

        statements = statements.stream()
            .map(
                statement -> {
                  if (statement instanceof MethodDeclaration method) {
                    var methodStatements = method.getBody().getStatements().stream().map(
                        it -> {
                          if (it instanceof MethodInvocation invocation
                              && isFromGenericDao(invocation)
                              && ((MethodInvocation)invocation.get()).getName().getSimpleName().equals("getEntityManager")) {
                            return new FieldAccess(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                target,
                                name,
                                null);
                          }
                          return it;
                        }).toList();
                    var newBody = method.getBody().withStatements(methodStatements);
                    return method.withBody(newBody);

                  }
                  return statement;
                })
            .toList();

        J.Block newBody = body.withStatements(statements);
        // Remove implements
        classDecl = classDecl.withImplements(Collections.emptyList());
        maybeAddImport(EntityManager.class.getCanonicalName(), false);
        maybeAddImport(PersistenceContext.class.getCanonicalName(), false);

        // Update the class declaration with the new body
        classDecl = classDecl.withBody(newBody);
      }
      return super.visitClassDeclaration(classDecl, ctx);
    }

    //    @Override
    //    public Statement visitStatement(Statement statement,
    //        final ExecutionContext executionContext) {
    //      var name = new JLeftPadded<>(Space.EMPTY, new J.Identifier(
    //          Tree.randomId(),
    //          Space.EMPTY,
    //          Markers.EMPTY,
    //          Collections.emptyList(),
    //          "entityManager",
    //          null,
    //          null
    //      ), Markers.EMPTY);
    //
    //      var target = new J.Identifier(
    //          Tree.randomId(),
    //          Space.EMPTY,
    //          Markers.EMPTY,
    //          Collections.emptyList(),
    //          "this",
    //          null,
    //          null
    //      );
    //      if (statement instanceof MethodInvocation method
    //          && isFromGenericDao(method)
    //          && method.getSimpleName().equals("getEntityManager")) {
    //        return new FieldAccess(
    //            Tree.randomId(),
    //            Space.EMPTY,
    //            Markers.EMPTY,
    //            target,
    //            name,
    //            null
    //        );
    //      }
    //      return super.visitStatement(statement, executionContext);
    //    }

    private boolean isFromGenericDao(final MethodInvocation method) {
      return true;
    }

    private static boolean entityManagerExists(final ClassDeclaration classDecl) {
      return classDecl.getBody().getStatements().stream()
          .anyMatch(
              it ->
                  it instanceof J.VariableDeclarations declarations
                      && declarations.getVariables().stream()
                          .anyMatch(
                              field ->
                                  field.getSimpleName().equals("entityManager")
                                      || field.getSimpleName().equals("em")));
    }

    private J.VariableDeclarations createEntityManagerField() {
      var persistenceContextAnnotation =
          new J.Annotation(
              Tree.randomId(),
              Space.format("\t"),
              Markers.EMPTY,
              new J.Identifier(
                  Tree.randomId(),
                  Space.EMPTY,
                  Markers.EMPTY,
                  Collections.emptyList(),
                  "PersistenceContext",
                  null,
                  null),
              null);

      TypeTree typeExpression =
          new J.Identifier(
              Tree.randomId(),
              Space.SINGLE_SPACE,
              Markers.EMPTY,
              Collections.emptyList(),
              "EntityManager",
              null,
              null);

      List<JRightPadded<NamedVariable>> variables =
          Collections.singletonList(
              new JRightPadded<>(
                  new J.VariableDeclarations.NamedVariable(
                      Tree.randomId(), // ID for the named variable
                      Space.SINGLE_SPACE, // Space Formatting
                      Markers.EMPTY, // Markers
                      new J.Identifier(
                          Tree.randomId(),
                          Space.EMPTY,
                          Markers.EMPTY,
                          Collections.emptyList(),
                          "entityManager", // Variable name
                          null, // Type (can be null)
                          null // Annotations (if any, otherwise null)
                          ),
                      Collections.emptyList(),
                      null,
                      // Initializer (null if no initializer)
                      null),
                  Space.EMPTY,
                  Markers.EMPTY));

      return new J.VariableDeclarations(
          Tree.randomId(),
          Space.format("\n"),
          Markers.EMPTY,
          List.of(persistenceContextAnnotation),
          List.of(
              new J.Modifier(
                  Tree.randomId(),
                  Space.format("\n\t"),
                  Markers.EMPTY,
                  "private",
                  Type.Private,
                  Collections.emptyList())),
          typeExpression,
          null,
          Collections.emptyList(),
          variables);
    }

    private boolean implementsInterface(ClassDeclaration classDecl) {
      return classDecl.getImplements() != null
          && Objects.requireNonNull(classDecl.getImplements()).stream()
              .anyMatch(
                  impl ->
                      impl instanceof J.ParameterizedType type
                          && type.getClazz().toString().equals("GenericDao"));
    }
  }
}
