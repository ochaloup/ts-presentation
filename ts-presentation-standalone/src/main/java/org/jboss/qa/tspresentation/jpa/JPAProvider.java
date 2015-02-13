package org.jboss.qa.tspresentation.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAProvider {
    public void doWork() {
        Map<String, String> jpaConfiguration = new HashMap<String, String>();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ResourceLocalJTAPersistenceUnit", jpaConfiguration);
        EntityManager entityManager = (EntityManager) emf.createEntityManager();

        entityManager.getTransaction().begin();

        TransactionPresentationEntity entity = new TransactionPresentationEntity();
        entity.setName("Franta");

        entityManager.persist(entity);
        entityManager.getTransaction().commit();
    }
}
