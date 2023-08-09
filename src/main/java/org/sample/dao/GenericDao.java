package org.sample.dao;

import java.io.Serializable;

@Deprecated
public interface GenericDao<T, ID extends Serializable> {
  /**
   * update an entity.
   *
   * @param entity the entity to update
   * @return the updated entity
   */
  T update(T entity);

  /**
   * delete an entity from the database
   *
   * @param entity the entity to delete
   */
  void delete(T entity);

  /**
   * persist an entity
   *
   * @param entity the entity to persist
   */
  void persist(T entity);
}
