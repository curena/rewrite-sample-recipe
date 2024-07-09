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

@AutoConfigure
@Value
@EqualsAndHashCode(callSuper = false)
public class NoExtendsGenericJpaDao extends Recipe {
  
      @Option(displayName = "Interface fully qualified name",
              description = "The fully qualified name of the interface to match.",
              example = "com.yourpackage.MyInterface")
      String interfaceName;
  
      @Override
      public String getDisplayName() {
          return "Add instance variable, replace method usage, and remove interface implementation";
      }
  
      @Override
      public String getDescription() {
          return "Adds a new instance variable to classes implementing a specific interface, replaces usages of a method with the new variable, and removes the interface implementation.";
      }
  
      @Override
      public TreeVisitor<?, ExecutionContext> getVisitor() {
          return new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                  
                  if (implementsInterface(cd, interfaceName)) {
                      cd = addInstanceVariable(cd);
                      cd = removeInterfaceImplementation(cd);
                      cd = cd.withBody(
                          new JavaTemplate("{{ any }}")
                              .apply(getCursor(), cd.getBody().getCoordinates().replaceBody(),
                                  cd.getBody().withStatements(
                                      ListUtils.map(cd.getBody().getStatements(),
                                          stmt -> replaceMethodUsages(stmt))
                                  )
                              )
                      );
                  }
                  
                  return cd;
              }
              
              private boolean implementsInterface(J.ClassDeclaration cd, String interfaceName) {
                  return cd.getImplements().stream()
                      .anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), interfaceName));
              }
              
              private J.ClassDeclaration addInstanceVariable(J.ClassDeclaration cd) {
                  return cd.withBody(cd.getBody().withStatements(ListUtils.concat(
                      cd.getBody().getStatements(),
                      new J.VariableDeclarations(
                          Tree.randomId(),
                          Space.EMPTY,
                          Markers.EMPTY,
                          emptyList(),
                          JavaType.buildType("jakarta.persistence.EntityManager"),
                          null,
                          emptyList(),
                          singletonList(
                              new J.VariableDeclarations.NamedVariable(
                                  Tree.randomId(),
                                  Space.EMPTY,
                                  Markers.EMPTY,
                                  J.Identifier.build(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "entityManager"),
                                  emptyList(),
                                  null,
                                  null
                              )
                          )
                      )
                  )));
              }
              
              private J.ClassDeclaration removeInterfaceImplementation(J.ClassDeclaration cd) {
                  return cd.withImplements(cd.getImplements().stream()
                      .filter(impl -> !TypeUtils.isOfClassType(impl.getType(), interfaceName))
                      .collect(toList()));
              }
              
              private J replaceMethodUsages(J j) {
                  return new JavaVisitor<ExecutionContext>() {
                      @Override
                      public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                          if (method.getName().getSimpleName().equals("myIntMethod") &&
                              method.getSelect() == null) {
                              return method.withName(method.getName().withSimpleName("myVar"));
                          }
                          return super.visitMethodInvocation(method, ctx);
                      }
                  }.visit(j, ctx);
              }
          };
      }
  }
