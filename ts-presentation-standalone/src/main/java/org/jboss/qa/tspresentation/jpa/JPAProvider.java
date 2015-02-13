package org.jboss.qa.tspresentation.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAProvider {
    public void doWork() {
        EntityManagerFactory emf;
        emf = Persistence.createEntityManagerFactory("ResourceLocalJTAPersistenceUnit");
        EntityManager entityManager = (EntityManager) emf.createEntityManager();

        entityManager.getTransaction().begin();

        TestEntity entity = new TestEntity();
        entity.setId(2);
        entity.setName("Franta");

        entityManager.persist(entity);
        entityManager.getTransaction().commit();
    }
}
