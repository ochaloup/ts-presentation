package org.jboss.qa.tspresentation.test.jpa.persistencexml;

import java.sql.SQLException;

import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.PersistenceUnitPersistBean;
import org.jboss.qa.tspresentation.test.jpa.JpaUtils;
import org.jboss.qa.tspresentation.utils.JdbcBean;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Conf:
 *     <persistence-unit name="TestPersistenceUnit" transaction-type="RESOURCE_LOCAL">
 *       <jta-data-source>java:jboss/datasource-test</jta-data-source>
 *       ...
 *
 * Behaviour:
 *  When you run any update operation then you have to start transaction manually by em.getTransaction().begin()
 *    otherwise you will experience exception that you are not part of transaction (if you will try to do flush())
 *    But! for such handling you need to disable TM from starting transaction by {@link TransactionAttribute} as {@link TransactionAttributeType#NOT_SUPPORTED}
 *    If global txn is started then you will get javax.ejb.EJBException: javax.persistence.RollbackException: Error while committing the transaction
 */
@RunWith(Arquillian.class)
public class ResourceLocalJtaDatasourceTestCase {
    private static final Logger log = LoggerFactory.getLogger(ResourceLocalJtaDatasourceTestCase.class);

    private static final String DEPLOYMENT = "resource-local-tag-jta-datasource-properties";

    @Inject PersistenceUnitPersistBean persistBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClass(PersistenceUnitPersistBean.class)
                .addClass(JBossTestEntity.class)
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class);

        String persistenceXml = JpaUtils.getJtaPuWithJtaTagAndJtaDsPersistenceXml();
        persistenceXml = JpaUtils.setAsResourceLocal(persistenceXml);
        persistenceXml = JpaUtils.addTag(persistenceXml, "<class>" + JBossTestEntity.class.getName() + "</class>\n");

        // persistence.xml in war should be placed in WEB-INF/classes/META-INF/persistence.xml
        war.addAsWebInfResource(new StringAsset(persistenceXml), "classes/META-INF/persistence.xml");
        return war;
    }

    @Test
    public void testPersist() throws SQLException {
        log.info("Running testPersist with name '{}'", DEPLOYMENT);
        int id = persistBean.doPersist(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        // ExampleDS is used so the defined ds will not be updated
        Assert.assertNull(name);
    }

    @Test
    public void testPersistWithNewTransaction() throws SQLException {
        log.info("Running testPersistWithNewTransaction with name '{}'", DEPLOYMENT);
        int id = persistBean.doPersistWithStartingTransaction(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        // ExampleDS is taken as source for entity manager (not properties)
        Assert.assertEquals(DEPLOYMENT, name);
    }

    /**
     * This is not working as creating EMF from Persistence is happening as JavaSE
     * and it's expects that <jta-data-source> is not used and that configuration
     * is part of <properties>
     */
    @Test(expected = EJBException.class)
    public void testPersistEmfManuallyCreated() throws SQLException {
        log.info("Running testPersistEmfManuallyCreated with name '{}'", DEPLOYMENT);
        persistBean.doPersistWithManualEmfCreation(DEPLOYMENT);
    }
}
