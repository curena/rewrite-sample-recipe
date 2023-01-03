package org.cecil;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.JavaParser.fromJavaVersion;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

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

class AddEntityManagerToJpaDaoTest implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new AddEntityManagerToJpaDao())
        .parser(fromJavaVersion().logCompilationWarningsAndErrors(true).classpath("main"));
  }

  @Test
  void visitVariableDeclarations_should_add_entityManager_after_static() {
    rewriteRun(
        java(
            """
            import contrast.teamserver.dao.TrustedDeviceDao;
            import contrast.teamserver.dao.TrustedDevice;

            public class TrustedDeviceJpaDao implements TrustedDeviceDao {
                private static final String kabob = "kabob";
                private final String myString = "foooooooo";
                public void doAThing(final TrustedDevice entity) {
                    getEntityManager().persist(entity);
                }
            }
        """,
            """
            import contrast.teamserver.dao.TrustedDeviceDao;
            import contrast.teamserver.dao.TrustedDevice;

            public class TrustedDeviceJpaDao implements TrustedDeviceDao {
                @Setter(onMethod = @__(@PersistenceContext), value = AccessLevel.PACKAGE)
                private EntityManager entityManager;
                private static final String kabob = "kabob";
                private final String myString = "foooooooo";
                public void doAThing(final TrustedDevice entity) {
                    getEntityManager().persist(entity);
                }
            }
        """));
  }

    @Test
    public void visitVariableDeclarations_should_add_entityManager_when_no_variables() {
        rewriteRun(
            java(
                """
                import contrast.teamserver.dao.TrustedDeviceDao;
                import contrast.teamserver.dao.TrustedDevice;
    
                public class TrustedDeviceJpaDao implements TrustedDeviceDao {
                    public void doAThing(final TrustedDevice entity) {
                        getEntityManager().persist(entity);
                    }
                }
            """,
                """
                import contrast.teamserver.dao.TrustedDeviceDao;
                import contrast.teamserver.dao.TrustedDevice;
    
                public class TrustedDeviceJpaDao implements TrustedDeviceDao {
                    @Setter(onMethod = @__(@PersistenceContext), value = AccessLevel.PACKAGE)
                    private EntityManager entityManager;
                    
                    public void doAThing(final TrustedDevice entity) {
                        getEntityManager().persist(entity);
                    }
                }
            """));
    }
}
