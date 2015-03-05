package org.jboss.qa.tspresentation.test.jpa;

import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.SimpleJPABean;
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
 * Conf:
 *     <persistence-unit name="TestPersistenceUnit" transaction-type="JTA">
 *       <properties>
 *           <property name="hibernate.connection.url" value=""/>
 *           <property name="hibernate.connection.driver_class" value=""/>
 *           <property name="hibernate.connection.username" value=""/>
 *           <property name="hibernate.connection.password" value=""/>
 *           ...
 * Behaviour:
 *  As it's defined as JTA then hibernate properties are ignored. As jta-data-source tag
 *  is not defined so the persistence unit is expected to be configured for default datasource
 *  which is in default configuration ExampleDS
 */
@RunWith(Arquillian.class)
public class JtaJdbcTestCase {
    private static final Logger log = LoggerFactory.getLogger(JtaJdbcTestCase.class);

    private static final String DEPLOYMENT = "jta-tag-jdbc-properties";

    @Inject SimpleJPABean simpleJpaBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = JpaUtils.getShrinkWrapJar(DEPLOYMENT);
        jar.addAsManifestResource(new StringAsset(JpaUtils.getJtaPuWithJdbcPropertiesDsPersistenceXml()), "persistence.xml");
        return jar;
    }

    @Test
    public void testPersist() throws SQLException {
        log.info("Running test with name '{}'", DEPLOYMENT);
        int id = simpleJpaBean.doPersist(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }
}
