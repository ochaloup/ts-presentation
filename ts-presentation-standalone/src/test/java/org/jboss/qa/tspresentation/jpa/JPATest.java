package org.jboss.qa.tspresentation.jpa;

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.jboss.qa.tspresentation.jdbc.JDBCDriver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JPATest {

    private static JPAProvider jpaProviderResourceLocal;
    private static ClassLoader originalContextClassloader;

    @BeforeClass
    public static void registerDriver() throws SQLException {
        // Loading JDBC driver from URL specified in properties file
        JDBCDriver.registerDriver();

        /*
         * This is a bit hack as ConnectionProvider of Hibernate does do Class.forName() to get driver class
         * as driver was loaded dynamically so it's not on application classpath.
         * Despite the fact the DriverManagerConnectionProviderImpl is used the DriverManager is used for
         * getting connection but it still loads the class by forName() method
         * When Class.forName() fails then there is a fallback to try to load it from ContextClassLoader
         * and we redefine it here to get it really loaded.
         * Some discussion at https://forum.hibernate.org/viewtopic.php?f=1&t=1002728
         * Another less hacky (probably) solution would be to use my own ConnectionProvider and force Hibernate
         * to use it with property hibernate.connection.provider_class in persistence.xml
         */
        originalContextClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(JDBCDriver.getDriverClassLoader());

        // creating entity manager factory
        jpaProviderResourceLocal = new JPAProvider("ResourceLocalJTAPersistenceUnit");
    }

    @AfterClass
    public static void restoreClassloader() {
        // Returning back the loader to not influence other test classes
        Thread.currentThread().setContextClassLoader(originalContextClassloader);
        // Closing entity manager factory
        jpaProviderResourceLocal.close();
    }

    @Test
    public void baseUsage() {
        EntityManager em = jpaProviderResourceLocal.getEntityManager();
        EntityTransaction tx = null;

        try {
            tx = em.getTransaction();
            tx.begin();

            TransactionPresentationEntity entity = new TransactionPresentationEntity();
            entity.setName("Frodo");

            em.persist(entity);
            tx.commit();
        } catch (RuntimeException re) {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            em.close();
        }
    }
}
