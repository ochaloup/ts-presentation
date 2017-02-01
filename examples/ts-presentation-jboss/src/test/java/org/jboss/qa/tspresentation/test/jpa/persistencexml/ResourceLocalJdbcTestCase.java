package org.jboss.qa.tspresentation.test.jpa.persistencexml;

import java.sql.SQLException;

import javax.inject.Inject;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.PersistenceUnitPersistBean;
import org.jboss.qa.tspresentation.test.jpa.JpaUtils;
import org.jboss.qa.tspresentation.utils.FileLoader;
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
 *       <class>org.jboss.qa.tspresentation.jpa.JBossTestEntity</class>
 *       <properties>
 *           <property name="hibernate.connection.url" value=""/>
 *           <property name="hibernate.connection.driver_class" value=""/>
 *           <property name="hibernate.connection.username" value=""/>
 *           <property name="hibernate.connection.password" value=""/>
 *           ...
 * Behaviour:
 *  When using resource local persistence unit then deployment can't contain bean which uses {@link PersistenceContext}
 *    otherwise the deployment fails.
 *  When you use {@link PersistenceUnit} then it does not reflect the properties and creates emf from default datasource
 *    which is by default ExampleDS
 */
@RunWith(Arquillian.class)
public class ResourceLocalJdbcTestCase {
    private static final Logger log = LoggerFactory.getLogger(ResourceLocalJdbcTestCase.class);

    private static final String DEPLOYMENT = "resource-local-tag-jdbc-properties";

    @Inject PersistenceUnitPersistBean persistBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClass(PersistenceUnitPersistBean.class)
                .addClass(JBossTestEntity.class)
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class); // constant definitions
        war.addAsLibraries(FileLoader.getFile(ProjectProperties.get(ProjectProperties.JDBC_DRIVER_FILEPATH)));

        String persistenceXml = JpaUtils.getJtaPuWithJdbcPropertiesDsPersistenceXml();
        persistenceXml = JpaUtils.setAsResourceLocal(persistenceXml);
        persistenceXml = JpaUtils.addTag(persistenceXml, "<class>" + JBossTestEntity.class.getName() + "</class>\n"); // this is need for manually created emf

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

        Assert.assertNull(name);
    }

    @Test
    public void testPersistWithNewTransaction() throws SQLException {
        log.info("Running testPersistWithNewTransaction with name '{}'", DEPLOYMENT);
        int id = persistBean.doPersistWithStartingTransaction(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        // ExampleDS is taken as source for entity manager (not properties)
        Assert.assertNull(name);
    }

    @Test
    public void testPersistEmfManuallyCreated() throws SQLException {
        log.info("Running testPersistEmfManuallyCreated with name '{}'", DEPLOYMENT);
        int id = persistBean.doPersistWithManualEmfCreation(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }
}
