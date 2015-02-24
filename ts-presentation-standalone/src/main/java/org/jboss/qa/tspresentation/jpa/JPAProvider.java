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
    public JPAProvider(final String persistenceUnitName, final Map<String,String> additionalConfiguration) {
        Map<String, String> jpaConfiguration = new HashMap<String, String>();
        jpaConfiguration.put("hibernate.connection.url", ProjectProperties.get(DB_URL));
        jpaConfiguration.put("hibernate.connection.driver_class", ProjectProperties.get(JDBC_CLASS));
        jpaConfiguration.put("hibernate.connection.username", ProjectProperties.get(DB_USERNAME));
        jpaConfiguration.put("hibernate.connection.password", ProjectProperties.get(DB_PASSWORD));
        if(additionalConfiguration != null) {
            jpaConfiguration.putAll(additionalConfiguration);
        }
        emf = Persistence.createEntityManagerFactory(persistenceUnitName, jpaConfiguration);
    }

    public JPAProvider(final Map<String,String> additionalConfiguration) {
        this(ProjectProperties.PERSISTENCE_UNIT_RESOURCE_LOCAL, additionalConfiguration);
    }

    public JPAProvider(final String persistenceUnitName) {
        this(persistenceUnitName, null);
    }

    public JPAProvider() {
        this(new HashMap<String, String>());
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
