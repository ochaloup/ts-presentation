package org.jboss.qa.tspresentation.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = PresentationEntity.TABLE_NAME)
public class PresentationEntity {
    public static final String TABLE_NAME = "PRESENTATION_ENTITY";
    public static final String NAME_COLUMN_NAME = "name";

    @Id
    @GeneratedValue
    @Column(name = "id")
    int id;

    @Column(name = NAME_COLUMN_NAME)
    String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
