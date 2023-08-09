package org.cecil.codytest;

import static org.openrewrite.Tree.randomId;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

@Value
@EqualsAndHashCode(callSuper = true)
public class MigrateFromGenericJpaDao extends Recipe {

  @Override
  public String getDisplayName() {
    return "Migrate from GenericJpaDao";
  }

  @Override
  public String getDescription() {
    return "Migrates classes extending GenericJpaDao by removing the extends clause, adding an EntityManager field, and replacing method calls.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<>() {

      @Override
      public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        if(classDecl.getExtends() != null &&
            classDecl.getExtends().getType().isAssignableFrom(Pattern.compile("org.sample.dao.GenericJpaDao"))) {

          // remove extends
          classDecl = classDecl.withExtends(null);

          // add entityManager field
          var body = classDecl.getBody();
          var statements = new ArrayList<>(body.getStatements());
          var entityManager = variableDeclarationsEntityManager();
          statements.add(0, entityManager);
          var bodyWithEntityManager = body.withStatements(statements);
          classDecl = classDecl.withBody(bodyWithEntityManager);
          classDecl = replaceGetEntityManager(classDecl);
        }

        return super.visitClassDeclaration(classDecl, ctx);
      }

      private J.ClassDeclaration replaceGetEntityManager(J.ClassDeclaration classDecl) {
        var methodInvocations = FindMethods.find(classDecl, "getEntityManager()");
        for (J methodInvocation : methodInvocations) {
          if (methodInvocation instanceof J.MethodInvocation invocation) {
            var statement = (Statement) invocation;
            var newStatement = JavaTemplate.builder("entityManager").build();
            var replacement = (Statement) newStatement.apply(getCursor(), invocation.getCoordinates().replace());
            classDecl = classDecl.withBody(classDecl.getBody().withStatements(
                classDecl.getBody().getStatements().stream()
                    .map(s -> s == statement ? replacement : s)
                    .toList()
            ));
          }
        }
        return classDecl;
      }

      private VariableDeclarations variableDeclarationsEntityManager() {
        J.VariableDeclarations.NamedVariable variable = new J.VariableDeclarations.NamedVariable(
            randomId(),
            Space.SINGLE_SPACE,
            Markers.EMPTY,
            TypeTree.build("entityManager"),
            List.of(),
            null,
            TypeTree.build("jakarta.persistence.EntityManager"));

        return new VariableDeclarations(
            randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            List.of(),
            List.of(new Modifier(randomId(), Space.EMPTY, Markers.EMPTY, null, Type.Private, List.of())),
            TypeTree.build("jakarta.persistence.EntityManager"),
            Space.EMPTY,
            List.of(),
            List.of(JRightPadded.build(variable)));
      }


    };
  }

}

