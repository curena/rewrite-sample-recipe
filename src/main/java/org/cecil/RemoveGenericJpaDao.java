package org.cecil;
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Recipe;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveGenericJpaDao extends Recipe {

  @Override
  public String getDisplayName() {
    return "Removes usage of deprecated `GenericJpaDao`";
  }

  @Override
  public String getDescription() {
    return """
      Removes all usages of the deprecated class `GenericJpaDao`, adds an `EntityManager`
      instance variable to the JPA DAO in question and replaces calls to `GenericDao#getEntityManager` with this
      variable.
    """;
  }

  public RemoveGenericJpaDao() {
    doNext(new NoExtendsGenericJpaDao());
    doNext(new AddEntityManagerToJpaDao());
    doNext(new ReplaceGetPersistentClass());
    doNext(new NoGenericDaoOverrides());
  }
}
