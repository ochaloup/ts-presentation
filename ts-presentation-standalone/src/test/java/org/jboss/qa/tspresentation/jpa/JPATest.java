package org.jboss.qa.tspresentation.jpa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaDelete;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.jboss.qa.tspresentation.jdbc.JDBCDriver;
import org.jboss.qa.tspresentation.jdbc.JdbcTest;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPATest {
    private static final Logger log = LoggerFactory.getLogger(JPATest.class);

    private static JPAProvider jpaResourceLocal, jpaJTA;
    private static ClassLoader originalContextClassloader;

    private static final String NAME = "Frodo Baggins";

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
        jpaResourceLocal = new JPAProvider(ProjectProperties.PERSISTENCE_UNIT_RESOURCE_LOCAL);
        jpaJTA = new JPAProvider(ProjectProperties.PERSISTENCE_UNIT_JTA);
    }

    @AfterClass
    public static void restoreClassloader() {
        // Returning back the loader to not influence other test classes
        Thread.currentThread().setContextClassLoader(originalContextClassloader);
        // Closing entity manager factory
        jpaResourceLocal.close();
    }

    @After
    public void deleteAllData() {
        EntityManager em = jpaResourceLocal.getEntityManager();
        try {
            em.getTransaction().begin();
            CriteriaDelete<PresentationEntity> criteriaDeleteAll = em.getCriteriaBuilder()
                    .createCriteriaDelete(PresentationEntity.class);
            criteriaDeleteAll.from(PresentationEntity.class);
            em.createQuery(criteriaDeleteAll).executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Test
    public void persistWithTransaction() throws SQLException {
        EntityManager em = jpaResourceLocal.getEntityManager();
        EntityTransaction tx = null;
        int id = -1;

        try {
            tx = em.getTransaction();
            tx.begin(); // it also calls Connection.setAutoCommit(false)

            PresentationEntity entity = new PresentationEntity();
            entity.setName(NAME);

            em.persist(entity);
            em.flush();

            // checking if database state was changed (from different connection)
            Assert.assertNull(selectById(id));

            id = entity.getId();
            tx.commit();
        } catch (RuntimeException re) {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            em.close();
        }

        // just checking if database state was changed
        Assert.assertEquals(NAME, selectById(id));
    }

    /**
     * !!! DATABASE dependent !!!
     *
     * When Hibernate starts transaction it automatically calls {@link Connection#setAutoCommit(boolean)}
     * with false. If there is no transaction specified then EntityManager closes it on EntityManager.close().
     * Now it depends on DB behaviour
     *  - PostgreSQL - Connection.close() => rollback
     *  - Oracle - Connection.close() => commit
     *
     * To get data being commmited it's should  be necessary to set
     * hibernate.connection.autocommit as true.
     *
     * For longer discussion on this see: https://developer.jboss.org/wiki/Non-transactionalDataAccessAndTheAuto-commitMode
     *
     * BUT!!! for PostgreSQL
     *        if I set hibernate.connection.autocommit to true then it does not behaves as I would expect
     *        data is not pass to database at all - em.close() does not do that and em.flush() is not permitted outside of txn
     *        I have idea that this option was disabled in Hibernate 4.x but I'm not sure in this at all
     */
    @Test
    public void persistNoTransaction() throws SQLException {
        EntityManager em = jpaResourceLocal.getEntityManager();

        // gettting Hibernate session and checking autocommit mode of the connection
        Session session = (Session) em.getDelegate();
        session.doWork(
            new Work() {
                public void execute(final Connection connection) throws SQLException {
                    if(connection != null) {
                        log.info("Connection autocommit mode is " + (connection.getAutoCommit() ? "true" : "false"));
                    }
                }
            }
        );

        int id = -1;
        try {
            PresentationEntity entity = new PresentationEntity();
            entity.setName(NAME);

            // em.persist() => Hibernate remembers it has to make a database INSERT,
            // but does not actually execute the instruction until you commit the transaction
            // this is mainly done for performance reasons.
            em.persist(entity);

            id = entity.getId();
            Assert.assertNull(NAME, selectById(id));
        } finally {
            // em.flush(); // flush fails as it needs a running transaction (there is a check in code for this)
            em.close();
        }
        // as autocommit hibernate property hibernate.connection.autocommit is set to false by default
        // then there will be no information if Connection.close() rollbacks DB transaction
        Assert.assertNull(NAME, selectById(id));
    }

    /**
     * When persistence unit is defined as jta then the EntityManager tries to join existing transaction.
     * But as there is no such thing then the try fails.
     *
     * persistence.xml - transaction-type => default to JTA in a JavaEE environment and to RESOURCE_LOCAL in a JavaSE environment.
     * See http://stackoverflow.com/a/17331863/187035
     *
     * at org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.getStatus(JtaStatusHelper.java:76)
     * at org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isActive(JtaStatusHelper.java:118)
     * at org.hibernate.engine.transaction.internal.jta.CMTTransaction.join(CMTTransaction.java:149)
     * at org.hibernate.jpa.spi.AbstractEntityManagerImpl.joinTransaction(AbstractEntityManagerImpl.java:1602)
     * at org.hibernate.jpa.spi.AbstractEntityManagerImpl.postInit(AbstractEntityManagerImpl.java:210)
     * ...
     */
    @Test(expected = NullPointerException.class)
    public void jtaPersistenceUnitWithoutTM() {
        // :)
        jpaJTA.getEntityManager();
    }

    /**
     * Will return first data (name) from database based on id from table
     * {@link PresentationEntity#TABLE_NAME}. Using jdbc connection.
     */
    private String selectById(final int id) {
        try (Connection conn = JDBCDriver.getConnection()) {
            return JdbcTest.selectById(id, PresentationEntity.TABLE_NAME, PresentationEntity.NAME_COMUMN_NAME, conn);
        } catch (SQLException sqle) {
            throw new RuntimeException("Can't get data from " + PresentationEntity.TABLE_NAME + " where id is " + id, sqle);
        }
    }
}