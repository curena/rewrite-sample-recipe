package org.cecil.codytest;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class MigrateFromGenericJpaDaoTest implements RewriteTest {
  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new MigrateFromGenericJpaDao())
        .parser(
            JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
  }

  @Test
  public void migrate() {
    rewriteRun(
        java(
            """
          package org.sample.dao;

          public class BookJpaDao extends GenericJpaDao<Book, Long> {
            public void add(Book book) {
              getEntityManager().persist(book);
            }
          }


      """,
            """
           package org.sample.dao;

           import jakarta.persistence.EntityManager;
           
           public class BookJpaDao {
             private EntityManager entityManager;
             
             public void add(Book book) {
               entityManager.persist(book);
             }
           }
      """));
  }


}
