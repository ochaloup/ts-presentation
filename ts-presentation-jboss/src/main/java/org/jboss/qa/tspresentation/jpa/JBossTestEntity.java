package org.jboss.qa.tspresentation.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = JBossTestEntity.TABLE_NAME)
public class JBossTestEntity {
    public static final String TABLE_NAME = "JBOSS_TEST_ENTITY";
    public static final String NAME_COLUMN_NAME = "name";

    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = NAME_COLUMN_NAME)
    private String name;


    public JBossTestEntity() {
        // no-arg constructor needed for reflection
    }

    public JBossTestEntity(final String name) {
        setName(name);
    }

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
        if (!(obj instanceof JBossTestEntity))
            return false;
        JBossTestEntity other = (JBossTestEntity) obj;
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
