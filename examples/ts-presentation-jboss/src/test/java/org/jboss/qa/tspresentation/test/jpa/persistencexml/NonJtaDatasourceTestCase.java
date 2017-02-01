package org.jboss.qa.tspresentation.test.jpa.persistencexml;

import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.PersistenceContextPersistBean;
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
 * Conf:
 *     <persistence-unit name="TestPersistenceUnit" transaction-type="JTA">
 *       <non-jta-data-source>java:jboss/datasource-test</non-jta-data-source>
 *       ...
 * Behaviour:
 *  Datasource is jta so it is managed by TM. It does not matter if <non-jta-data-source> tag
 *  is used.
 */
@RunWith(Arquillian.class)
public class NonJtaDatasourceTestCase {
    private static final Logger log = LoggerFactory.getLogger(NonJtaDatasourceTestCase.class);

    private static final String DEPLOYMENT = "non-jta-tag-jta-datasource";

    @Inject PersistenceContextPersistBean simpleJpaBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = JpaUtils.getShrinkWrapJar(DEPLOYMENT);
        jar.addAsManifestResource(new StringAsset(JpaUtils.getJtaPuWithNonJtaTagAndJtaDsPersistenceXml()), "persistence.xml");
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
