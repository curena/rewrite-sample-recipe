package contrast.teamserver.dao;

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

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import lombok.Getter;

@Deprecated
public abstract class GenericJpaDao<EntityType, ID extends Serializable>
    implements GenericDao<EntityType, ID> {

  // ~ Instance fields
  // --------------------------------------------------------

  @Getter private Class<EntityType> persistentClass;

  /**
   * @deprecated Consider creating a global variable in each DAO implementation instead of using
   *     this field
   */

  // ~ Constructors
  // -----------------------------------------------------------

  @SuppressWarnings("unchecked")
  public GenericJpaDao() {
    if (getClass().getGenericSuperclass() instanceof Class) {
      this.persistentClass = (Class<EntityType>) getClass().getGenericSuperclass();
    } else if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
      this.persistentClass =
          (Class<EntityType>)
              ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
  }

  // ~ Methods
  // ----------------------------------------------------------------

  @Override
  public void delete(final EntityType entity) {
    getEntityManager().remove(entity);
  }

  @Override
  public EntityType update(final EntityType entity) {
    return getEntityManager().merge(entity);
  }

  @Override
  public void persist(final EntityType entity) {
    getEntityManager().persist(entity);
  }

  public EntityManager getEntityManager() {
    return new EntityManager();
  }
  class EntityManager {
    public void persist(EntityType entity) {}

    public EntityType merge(EntityType entity) {
      return entity;
    }

    public void remove(EntityType entity) {}
  }

}
