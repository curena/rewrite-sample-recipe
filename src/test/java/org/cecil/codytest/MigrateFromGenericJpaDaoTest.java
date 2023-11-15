package org.cecil.codytest;

import static org.openrewrite.java.Assertions.java;

import org.cecil.NoExtendsGenericJpaDao;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class MigrateFromGenericJpaDaoTest implements RewriteTest {
  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new NoExtendsGenericJpaDao())
        .parser(
            JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
  }

  @Test
  void migrate() {
    rewriteRun(
        java(
            """
          package org.sample.dao;

          public class BookJpaDao implements GenericDao<Book, Long> {
            private String myString;
          
            public void add(Book book) {
              getEntityManager().persist(book);
            }
          }


      """,
            """
           package org.sample.dao;

           import jakarta.persistence.EntityManager;
           import jakarta.persistence.PersistenceContext;
           
           public class BookJpaDao {
             @PersistenceContext
             private EntityManager entityManager;
             private String myString;
             
             public void add(Book book) {
               entityManager.persist(book);
             }
           }
      """));
  }


}
