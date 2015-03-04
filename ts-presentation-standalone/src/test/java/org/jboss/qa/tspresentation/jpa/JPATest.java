package org.jboss.qa.tspresentation.jpa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaDelete;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.jdbc.ReturningWork;
import org.jboss.qa.tspresentation.jdbc.JdbcDriver;
import org.jboss.qa.tspresentation.jdbc.JdbcUtil;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JPATest {
    private static final Logger log = LoggerFactory.getLogger(JPATest.class);

    private static JpaProvider jpaResourceLocal, jpaJTA;
    private static ClassLoader originalContextClassloader;

    private static final String NAME = "Frodo Baggins";
    private static final String OTHER_NAME = "Bilbo Baggins";

    @BeforeClass
    public static void registerDriver() throws SQLException {
        // Loading JDBC driver from URL specified in properties file
        JdbcDriver.registerDriver();

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
        Thread.currentThread().setContextClassLoader(JdbcDriver.getDriverClassLoader());

        // creating entity manager factory
        jpaResourceLocal = new JpaProvider(ProjectProperties.PERSISTENCE_UNIT_RESOURCE_LOCAL);
        jpaJTA = new JpaProvider(ProjectProperties.PERSISTENCE_UNIT_JTA);
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

    /**
     * Base usage
     */
    @Test
    public void persistWithTransaction() throws SQLException {
        EntityManager em = jpaResourceLocal.getEntityManager();
        EntityTransaction tx = null;
        PresentationEntity entity = new PresentationEntity();

        try {
            tx = em.getTransaction();
            tx.begin(); // it also calls Connection.setAutoCommit(false)

            entity.setName(NAME);

            em.persist(entity);
            em.flush();

            // checking if database state was changed (from different connection)
            Assert.assertNull(selectById(entity.getId()));

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
        Assert.assertEquals(NAME, selectById(entity.getId()));
    }

    /**
     * When Hibernate starts transaction it automatically calls {@link Connection#setAutoCommit(boolean)} with false.
     * If we are out of transaction then autocommit is false as well until we set hibernate.connection.autocommit property.
     * For longer discussion on this see: https://developer.jboss.org/wiki/Non-transactionalDataAccessAndTheAuto-commitMode
     *
     * As we are out of transaction then no update, insert etc. will be reflected to a SQL statement.
     * There is in fact nothing to be done - persist is silently ignored.
     */
    @Test
    public void persistNoTransaction() throws SQLException {
        JpaProvider autocommitJpaProvider = getAutocommitTrueJPAProvider();
        EntityManager em = autocommitJpaProvider.getEntityManager();
        autocommitAssert(em, true);

        int id = -1;
        try {
            PresentationEntity entity = new PresentationEntity();
            entity.setName(NAME);

            // em.persist() => Hibernate remembers it has to make a database INSERT,
            // but does not actually execute the instruction until you commit the transaction or call flush() operation
            // this is mainly done for performance reasons (lazy DML)
            em.persist(entity);

            // id is set by doing select to database (generated value of @Id) -> select statement could be run out of the transaction
            id = entity.getId();
            // no actual change in db occurs
            Assert.assertNull(selectById(id));
        } finally {
            // em.flush(); // flush fails as it needs a running transaction (there is a check in code for this)
            this.autocommitAssert(em, true); // expecting autocommit is still true
            em.close(); // nothing is done as no transaction is active
            autocommitJpaProvider.close(); // to having a clean garden
        }
        // still nothing in database :)
        Assert.assertNull(selectById(id));
    }

    /**
     * For "JDBC based" EntityTransaction the {@link AbstractTransactionImpl#doBegin()}
     * all the time change the autocommit mode to false and does not reflect the settings
     * of property hibernate.connection.autocommit
     * This property is taken in settings property interface {@link AvailableSettings#AUTOCOMMIT}
     *
     * Not sure when and how is this property reflected.
     */
    @Ignore
    @Test
    public void transactionWithAutocommitTrue() throws SQLException {
        JpaProvider autocommitJpaProvider = getAutocommitTrueJPAProvider();
        EntityManager em = autocommitJpaProvider.getEntityManager();
        autocommitAssert(em, true);

        PresentationEntity entity = new PresentationEntity(NAME);
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            // autocommit mode is change everytime and the hibernate.connection.autocommit is not reflected
            autocommitAssert(em, true);

            em.persist(entity);
            Assert.assertNull("Persist does not do any change of database", selectById(entity.getId()));
            em.flush();
            Assert.assertEquals("Data should be in DB as we use autocommit=true", NAME, selectById(entity.getId()));
            tx.commit();
        } catch (RuntimeException re) {
            doRollback(tx);
            throw re;
        } finally {
            em.close();
            autocommitJpaProvider.close();
        }
    }

    /**
     * What happens when close on enity manager is called without finishing the transaction?
     *
     * It depends on DB behaviour
     *  - PostgreSQL - Connection.close() => rollback
     *  - Oracle - Connection.close() => commit
     */
    @Test
    public void transactionEndsByCallOfEntityManagerClose() {
        EntityManager em = jpaResourceLocal.getEntityManager();
        PresentationEntity entity = null;

        try {
            em.getTransaction().begin();
            entity = new PresentationEntity();
            entity.setName(NAME);
            em.persist(entity);
        } finally {
            em.close();
        }

        Assert.assertNull("On postgres rollback is called so there will be nothing saved in database",
                selectById(entity.getId()));
    }

    @Test
    public void multipleTransactions() throws SQLException {
        EntityTransaction tx = null;
        Connection conn = null, conn2 = null;
        PresentationEntity entity = null;
        final String newName = NAME + "-changed";

        EntityManager em = jpaResourceLocal.getEntityManager();

        try {
            tx = em.getTransaction();
            tx.begin();

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

    @Test
    public void findWithoutTransactionAndUpdateWithTransaction() throws SQLException {
        // creating test data - entity which will be retrieved afterwards
        PresentationEntity testSetupEntity = createPresentationEntity(NAME);

        // new em and finding entity by id
        EntityManager em = jpaResourceLocal.getEntityManager();
        PresentationEntity entity = em.find(PresentationEntity.class, testSetupEntity.id);
        // equals has to be the same
        Assert.assertEquals(testSetupEntity, entity);

        // this call makes the entity detached which means that no update on the enity will
        // be taken into consideration
        // a side note: when JTA is used the entity won't be attached to persistence context
        // entity found out of transaction context will be behaving as here when we cleared the em
        em.clear();

        // now updating the retrieved entity out of transaction in a transaction
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            entity = em.merge(entity); // entity was detached - we need to attach it again
            entity.setName(OTHER_NAME);
            tx.commit();
        } catch (RuntimeException re) {
            if(tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw re;
        } finally {
            em.close();
        }

        // retrieving entity from database (different em created)
        EntityManager emVerify = jpaResourceLocal.getEntityManager();
        try {
            PresentationEntity entityVerify = emVerify.find(PresentationEntity.class, testSetupEntity.id);
            emVerify.refresh(entityVerify); // this is ambiguous
            Assert.assertEquals(entityVerify, entity);
        } finally {
            emVerify.close();
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
        try (Connection conn = JdbcDriver.getConnection()) {
            return JdbcUtil.selectById(id, PresentationEntity.TABLE_NAME, PresentationEntity.NAME_COLUMN_NAME, conn);
        } catch (SQLException sqle) {
            throw new RuntimeException("Can't get data from " + PresentationEntity.TABLE_NAME + " where id is " + id, sqle);
        }
    }

    private int updateById(final int id, final String newName) {
        try (Connection conn = JdbcDriver.getConnection()) {
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

    private PresentationEntity createPresentationEntity(final String name) {
        EntityManager em = jpaResourceLocal.getEntityManager();
        EntityTransaction tx = null;
        PresentationEntity entity = null;

        try {
            tx = em.getTransaction();
            tx.begin();
            entity = new PresentationEntity();
            entity.setName(name);
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
        return entity;
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

    private void doRollback(final EntityTransaction txn) {
        if(txn != null && txn.isActive()) {
            txn.rollback();
        }
    }

    private JpaProvider getAutocommitTrueJPAProvider() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("hibernate.connection.autocommit", "true");
        JpaProvider autocommitTrueJpaProvider = new JpaProvider(ProjectProperties.PERSISTENCE_UNIT_RESOURCE_LOCAL, config);
        return autocommitTrueJpaProvider;
    }
}