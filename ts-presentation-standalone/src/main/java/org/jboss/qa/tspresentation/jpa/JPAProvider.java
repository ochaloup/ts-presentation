package org.jboss.qa.tspresentation.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jboss.qa.tspresentation.utils.ProjectProperties;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.*;

public class JPAProvider {

    private EntityManagerFactory emf;

    /**
     * Creating {@link EntityManagerFactory}
     */
    public JPAProvider(final String persistenceUnitName) {
        Map<String, String> jpaConfiguration = new HashMap<String, String>();
        jpaConfiguration.put("hibernate.connection.url", ProjectProperties.get(DB_URL));
        jpaConfiguration.put("hibernate.connection.driver_class", ProjectProperties.get(JDBC_CLASS));
        jpaConfiguration.put("hibernate.connection.username", ProjectProperties.get(DB_USERNAME));
        jpaConfiguration.put("hibernate.connection.password", ProjectProperties.get(DB_PASSWORD));
        emf = Persistence.createEntityManagerFactory(persistenceUnitName, jpaConfiguration);
    }

    public JPAProvider() {
        this("ResourceLocalJTAPersistenceUnit");
    }

    public EntityManager getEntityManager() {
        return (EntityManager) emf.createEntityManager();
    }

    /**
     * Closing {@link EntityManagerFactory}
     */
    public void close() {
        emf.close();
    }
}
