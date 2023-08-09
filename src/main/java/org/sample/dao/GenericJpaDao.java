package org.sample.dao;

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
