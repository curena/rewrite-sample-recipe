package org.sample.dao;

public class BookJpaDao extends GenericJpaDao<Book, Long> {
  public void add(Book book) {
    getEntityManager().persist(book);
  }
}
