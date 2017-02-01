package org.jboss.qa.tspresentation.jpa;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean which injects {@link EntityManagerFactory} with {@link PersistenceUnit}
 */
@Stateless
public class PersistenceUnitPersistBean {
    private static final Logger log = LoggerFactory.getLogger(PersistenceUnitPersistBean.class);

    private static final String PERSISTENCE_UNIT_NAME = "TestPersistenceUnit";

    @PersistenceUnit
    EntityManagerFactory emf;

    public int doPersist(final String name) {
        log.info("Running doPersist method with name '{}'", name);

        EntityManager em = emf.createEntityManager();
        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        return entity.getId();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int doPersistWithStartingTransaction(final String name) {
        log.info("Running doPersistWithStartingTransaction method with name '{}'", name);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        em.getTransaction().commit();
        return entity.getId();
    }

    public int doPersistWithManualEmfCreation(final String name) {
        log.info("Running doPersistWithManualEmfCreation method with name '{}'", name);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = emf.createEntityManager();

        // if we do not start transaction manually then nothing is saved to DB as there is no
        // transaction where persist could be done
        em.getTransaction().begin();
        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        em.getTransaction().commit();
        return entity.getId();
    }

    /**
     * It's possible to set synchronization only for JTA transaction-type.
     * If RESOURCE_LOCAL is used then you will get exception
     * javax.ejb.EJBException: java.lang.IllegalStateException: Illegal attempt to specify a SynchronizationType when building an EntityManager from a EntityManagerFactory defined as RESOURCE_LOCAL
     *
     * This is probably the same as {@link #doPersist(String)} when transaction is not started.
     */
    public int doPersistSynchronized(final String name) {
        log.info("Running doPersistSynchronized method with name '{}'", name);

        EntityManager em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED);
        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        return entity.getId();
    }

    public int doPersistUnsynchronized(final String name) {
        log.info("Running doPersistUnsynchronized method with name '{}'", name);

        // we are part of persistence unit which is transaction-type JTA but we refuses to use
        // the JTA handling for this emf by default and that we define what transaction we
        // want to add to (as we are JTA type we can't use em.getTransaction().begin())
        EntityManager em = emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED);
        JBossTestEntity entity = new JBossTestEntity(name);
        em.persist(entity);
        em.joinTransaction(); // joining current global transaction
        return entity.getId();
    }
}
