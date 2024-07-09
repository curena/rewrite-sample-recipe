package org.cecil.codytest;

import static org.openrewrite.java.Assertions.java;

import org.cecil.NoExtendsGenericJpaDao;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class MigrateFromGenericJpaDaoTest implements RewriteTest {

    @Test
    void addInstanceVariableAndReplaceMethodUsage() {
        rewriteRun(
          spec -> spec.recipe(new AddInstanceVariableAndReplaceMethodUsage("org.test.MyInterface")),
          java(
            """
              package org.test;
              
              public class TestClass implements MyInterface {
                  public MyVar myIntMethod() {
                      return new MyVar();
                  }
                  
                  public void doSomething() {
                      myIntMethod().doSomething();
                  }
              }
              """,
            """
              package org.test;
              
              public class TestClass {
                  private MyVar myVar;
                  
                  public void doSomething() {
                      myVar.doSomething();
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotModifyNonImplementingClass() {
        rewriteRun(
          spec -> spec.recipe(new AddInstanceVariableAndReplaceMethodUsage("org.test.MyInterface")),
          java(
            """
              package org.test;
              
              public class TestClass {
                  public void doSomething() {
                      // Some code
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesMultipleMethodUsages() {
        rewriteRun(
          spec -> spec.recipe(new AddInstanceVariableAndReplaceMethodUsage("org.test.MyInterface")),
          java(
            """
              package org.test;
              
              public class TestClass implements MyInterface {
                  public MyVar myIntMethod() {
                      return new MyVar();
                  }
                  
                  public void doSomething() {
                      myIntMethod().doSomething();
                      myIntMethod().doSomethingElse();
                  }
              }
              """,
            """
              package org.test;
              
              public class TestClass {
                  private MyVar myVar;
                  
                  public void doSomething() {
                      myVar.doSomething();
                      myVar.doSomethingElse();
                  }
              }
              """
          )
        );
    }
}
