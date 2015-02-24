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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PresentationEntity))
            return false;
        PresentationEntity other = (PresentationEntity) obj;
        if (id != other.getId())
            return false;
        if (name == null) {
            if (other.getName() != null)
                return false;
        } else if (!name.equals(other.getName()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("[%s] name: %s", getId(), getName());
    }
}
