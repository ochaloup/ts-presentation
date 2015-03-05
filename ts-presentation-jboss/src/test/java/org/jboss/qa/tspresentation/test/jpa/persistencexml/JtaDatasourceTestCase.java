package org.jboss.qa.tspresentation.test.jpa.persistencexml;

import java.sql.SQLException;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.PersistenceContextPersistBean;
import org.jboss.qa.tspresentation.jpa.PersistenceUnitPersistBean;
import org.jboss.qa.tspresentation.test.jpa.JpaUtils;
import org.jboss.qa.tspresentation.utils.JdbcBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is set of tests which consist of classes
 * {@link JtaDatasourceTestCase}
 * {@link JtaNonJtaDatasourceTestCase}
 * {@link NonJtaDatasourceTestCase}
 * {@link NonJtaNonJtaDatasourceTestCase}
 * {@link JtaJdbcTestCase}
 *
 * These cases just persists an entity but with differently settings of persistence.xml
 * where tag jta-data-source and non-jta-data-source is used
 * and where different type of datasource is used - either jta one (jta=true) or non-jta one (jta=false)
 *
 * For a nice summary of difference between JTA and RESOURCE_LOCAL see the answer on stackoverflow:
 * http://stackoverflow.com/questions/17331024/persistence-xml-different-transaction-type-attributes
 *
 * Conf:
 *     <persistence-unit name="TestPersistenceUnit" transaction-type="JTA">
 *       <jta-data-source>java:jboss/datasource-test</jta-data-source>
 *       ...
 * Behaviour:
 *  Datasource is jta so it is managed by TM
 */
@RunWith(Arquillian.class)
public class JtaDatasourceTestCase {
    private static final Logger log = LoggerFactory.getLogger(JtaDatasourceTestCase.class);

    private static final String DEPLOYMENT = "jta-tag-jta-datasource";

    @Inject PersistenceContextPersistBean jpaBean;

    @Inject PersistenceUnitPersistBean resourceLocalBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = JpaUtils.getShrinkWrapJar(DEPLOYMENT);
        jar.addAsManifestResource(new StringAsset(JpaUtils.getJtaPuWithJtaTagAndJtaDsPersistenceXml()), "persistence.xml");
        return jar;
    }

    @Test
    public void testPersist() throws SQLException {
        log.info("Running test with name '{}'", DEPLOYMENT);
        int id = jpaBean.doPersist(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }

    @Test
    public void testPersistWithPersistenceUnit() throws SQLException {
        log.info("Running testPersistWithPersistenceUnit with name '{}'", DEPLOYMENT);
        int id = resourceLocalBean.doPersist(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }

    /**
     * This will fail as TM started transaction and in such case the EM is not permitted to
     * start its own transaction.
     */
    @Test(expected = EJBException.class)
    public void testPersistWithPersistenceUnitWhereEmStartsTransaction() throws SQLException {
        log.info("Running testPersistWithPersistenceUnitWhereEmStartsTransaction with name '{}'", DEPLOYMENT);
        resourceLocalBean.doPersistWithStartingTransaction(DEPLOYMENT);
    }

    @Test
    public void testPersistWithEmfSynchronized() throws SQLException {
        log.info("Running testPersistWithEmfSynchronized with name '{}'", DEPLOYMENT);
        int id = resourceLocalBean.doPersistSynchronized(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }

    @Test
    public void testPersistWithEmfUnsynchronized() throws SQLException {
        log.info("Running testPersistWithEmfUnsynchronized with name '{}'", DEPLOYMENT);
        int id = resourceLocalBean.doPersistUnsynchronized(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }

    /**
     * Getting wrapped {@link NullPointerException} as it's not able to find entity manager factory in Persitence
     */
    @Test(expected = EJBException.class)
    public void testPersistWithManuallyCreatedEmf() throws SQLException {
        log.info("Running testPersistWithManuallyCreatedEmf with name '{}'", DEPLOYMENT);
        resourceLocalBean.doPersistWithManualEmfCreation(DEPLOYMENT);
    }
}
