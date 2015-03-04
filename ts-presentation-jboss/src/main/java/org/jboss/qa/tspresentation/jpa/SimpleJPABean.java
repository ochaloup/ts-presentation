package org.jboss.qa.tspresentation.jpa;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class SimpleJPABean {
    private static final Logger log = LoggerFactory.getLogger(SimpleJPABean.class);

    @PersistenceContext
    EntityManager em;

    public int doPersist(final String name) {
        log.info("Running doPersist method with name '{}'", name);

        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        return entity.getId();
    }
}
