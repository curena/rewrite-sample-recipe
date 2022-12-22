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
