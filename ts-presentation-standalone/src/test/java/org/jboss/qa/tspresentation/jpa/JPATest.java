package org.jboss.qa.tspresentation.jpa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaDelete;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.jboss.qa.tspresentation.jdbc.JDBCDriver;
import org.jboss.qa.tspresentation.jdbc.JdbcUtil;
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
    private static final String OTHER_NAME = "Bilbo Baggins";

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
     * This is DATABASE dependent
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

        autocommitAssert(em, true);

        int id = -1;
        try {
            PresentationEntity entity = new PresentationEntity();
            entity.setName(NAME);

            // em.persist() => Hibernate remembers it has to make a database INSERT,
            // but does not actually execute the instruction until you commit the transaction
            // this is mainly done for performance reasons.
            em.persist(entity);

            id = entity.getId();
            Assert.assertNull(selectById(id));
        } finally {
            // em.flush(); // flush fails as it needs a running transaction (there is a check in code for this)
            em.close();
        }
        // as autocommit hibernate property hibernate.connection.autocommit is set to false by default
        // then there will be no information if Connection.close() rollbacks DB transaction
        Assert.assertNull(selectById(id));
    }

    @Test
    public void multipleTransactions() throws SQLException {
        EntityTransaction tx = null;
        Connection conn = null, conn2 = null;
        PresentationEntity entity = null;
        final String newName = NAME + "-changed";

        EntityManager em = jpaResourceLocal.getEntityManager();
        autocommitAssert(em, true);

        try {
            tx = em.getTransaction();
            tx.begin();
            autocommitAssert(em, false);

            conn = getUnderlayingConnection(em);
            log.info("Using connection {}", conn);

            entity = new PresentationEntity();
            entity.setName(NAME);
            em.persist(entity);
            // saying if entity is loaded with all it's information or if we have just handle to it (proxy object)
            Assert.assertTrue("Entity was now persisted - it has to be loaded",
                    Persistence.getPersistenceUtil().isLoaded(entity));
            // saying if entity is attached or detached
            Assert.assertTrue("We are at the same transaction - entity has to be attached",
                    em.contains(entity));
            tx.commit();

            autocommitAssert(em, true);
            Assert.assertFalse("Expecting the connection being pooled and not being closed",
                    getUnderlayingConnection(em).isClosed());
            Assert.assertTrue("We are out of the context of transaction but with the resource local transaction " +
                    " and entity is detached only if we do it manually (e.g. by clear)",
                    em.contains(entity));


            tx = em.getTransaction();
            tx.begin();
            autocommitAssert(em, false);

            conn2 = getUnderlayingConnection(em);
            log.info("Using connection {}", conn2);
            Assert.assertTrue("We in context of other transaction - entity should be still attached",
                    em.contains(entity));

            em.clear();
            Assert.assertFalse("We expicitly called clear and entity should not be attached to persistence context by now",
                    em.contains(entity));

            // em.merge() seems is not working - I need to take entity which is returned by merge cal
            // merge creates new entity instance here but it seems to me being against spec... maybe?
            int id = entity.getId();
            entity = em.merge(entity);
            entity.setName(newName);
            Assert.assertEquals("Id of merged entity instance has to be the same", id, entity.getId());

            tx.commit();

            // Connections taken from pool and pool can return the same connection
            // Assert.assertNotEquals(conn, conn2);

        } catch (RuntimeException re) {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            em.close();
        }

        Assert.assertEquals("Database should contain value defined in second transaction",
                newName, selectById(entity.getId()));
    }

    /**
     * If we do some inserts or updates of database the JPA needs to be covered
     * by a database transaction. But if we do just select queries then we
     * do not need an active db transaction and hibernate will just do the query
     * right to database (if it's not in L1 cache a.k.a EntityManager or in L2 cache
     * a.ka. EntityManagerFactory)
     */
    @Test
    public void selectDoesNotNeedATransaction() throws SQLException {
        EntityManager em = jpaResourceLocal.getEntityManager();
        EntityTransaction tx = null;
        PresentationEntity entity = null;
        int id = -1;

        try {
            tx = em.getTransaction();
            tx.begin(); // it also calls Connection.setAutoCommit(false)

            entity = new PresentationEntity();
            entity.setName(NAME);
            em.persist(entity);
            id = entity.getId();

            tx.commit();
        } catch (RuntimeException re) {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw re;
        }
        // we are not closing the entity manager in finally block - we just leave it open for data being accesible

        // now we are out of transaction but context was not cleared
        // context is cleared when we use JTA in container
        Assert.assertTrue("Context was not cleared - em has to know the entity " + entity,
                em.contains(entity));
        // database was updated from different connection
        updateById(id, OTHER_NAME);
        // we are out of transaction but entity was not detached - getting new data from database
        em.refresh(entity);
        Assert.assertEquals(OTHER_NAME, entity.getName());
        // clearing entity manager by hand
        em.clear();
        // and running one more find to get the entity attached
        entity = em.find(PresentationEntity.class, id);
        Assert.assertTrue("Context was not cleared - em has to know the entity " + entity,
                em.contains(entity));
        // now we try to do update - setName for entity manager in memory
        entity.setName(NAME);
        // and write it to DB
        try {
            em.flush();
            Assert.fail("Writing to dabase needs a transaction but we do not start any - some error here around");
        } catch (TransactionRequiredException tre) {
            // this is expected - no transaction is running
        }
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
            return JdbcUtil.selectById(id, PresentationEntity.TABLE_NAME, PresentationEntity.NAME_COLUMN_NAME, conn);
        } catch (SQLException sqle) {
            throw new RuntimeException("Can't get data from " + PresentationEntity.TABLE_NAME + " where id is " + id, sqle);
        }
    }

    private int updateById(final int id, final String newName) {
        try (Connection conn = JDBCDriver.getConnection()) {
            String sqlUpdate = "UPDATE " + PresentationEntity.TABLE_NAME + " SET " + PresentationEntity.NAME_COLUMN_NAME + " = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, newName);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Can't update data in " + PresentationEntity.TABLE_NAME + " where id is " + id, sqle);
        }
    }

    private void autocommitAssert(final EntityManager em, final boolean autocommitValueExpected) throws SQLException {
        Connection conn =  getUnderlayingConnection(em);
        log.info("Connection autocommit mode is {}", (conn.getAutoCommit() ? "true" : "false"));
        Assert.assertEquals("Autocommit mode " + (autocommitValueExpected ? "true" : "false") + " is expected",
                autocommitValueExpected, conn.getAutoCommit());
    }

    private Connection getUnderlayingConnection(final EntityManager em) {
        // gettting Hibernate session as delegate from entity manager
        Session session = (Session) em.getDelegate();
        return session.doReturningWork(
            new ReturningWork<Connection>(){
                public Connection execute(final Connection connection) throws SQLException {
                    return connection;
                }
            }
        );
    }
}