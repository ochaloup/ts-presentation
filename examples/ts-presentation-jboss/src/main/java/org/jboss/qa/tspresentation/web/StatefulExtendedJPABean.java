package org.jboss.qa.tspresentation.web;


import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;

@Stateful
public class StatefulExtendedJPABean {
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    private JBossTestEntity entity;

    public void createEntity() {
        JBossTestEntity entity = new JBossTestEntity("my-name");
        em.persist(entity);
        this.entity = entity;
    }

    public void changeEntity() {
        // extended persistence context - entity is still attached
        entity.setName("my-new-name");
    }

    public JBossTestEntity findEntity() {
        String sql = "SELECT e FROM JBossTestEntity e WHERE id=:id";
        TypedQuery<JBossTestEntity> q = em.createQuery(sql, JBossTestEntity.class)
                .setParameter("id", entity.getId());
        return q.getSingleResult();
    }
}
