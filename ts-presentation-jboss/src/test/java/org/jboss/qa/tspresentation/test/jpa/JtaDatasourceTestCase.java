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

    @Inject SimpleJPABean simpleJpaBean;

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
        int id = simpleJpaBean.doPersist(DEPLOYMENT);

        log.info("Checking result of the persist action by jdbc query");
        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals(DEPLOYMENT, name);
    }
}
