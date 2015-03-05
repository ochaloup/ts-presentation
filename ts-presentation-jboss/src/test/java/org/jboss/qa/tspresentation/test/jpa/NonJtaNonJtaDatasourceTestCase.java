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
 *       <non-jta-data-source>java:jboss/nonjta-datasource-test</non-jta-data-source>
 *       ...
 * Behaviour:
 *  Datasource is non-jta (jta=false) so it is taken off the TM and all actions seem
 *  to be provided with autocommit = true (my feeling from the logs).
 *
 */
@RunWith(Arquillian.class)
public class NonJtaNonJtaDatasourceTestCase {
    private static final Logger log = LoggerFactory.getLogger(NonJtaNonJtaDatasourceTestCase.class);

    private static final String DEPLOYMENT = "non-jta-tag-non-jta-datasource";

    @Inject SimpleJPABean simpleJpaBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = JpaUtils.getShrinkWrapJar(DEPLOYMENT);
        jar.addAsManifestResource(new StringAsset(JpaUtils.getJtaPuWithNonJtaTagAndNonJtaDsPersistenceXml()), "persistence.xml");
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
