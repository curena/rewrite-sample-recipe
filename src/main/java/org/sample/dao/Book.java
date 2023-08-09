package org.sample.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Book {
  @Id
  @GeneratedValue
  private Long id;

  private String title;

  private String author;
}
