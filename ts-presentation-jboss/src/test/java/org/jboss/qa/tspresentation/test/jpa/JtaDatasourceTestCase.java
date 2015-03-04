package org.jboss.qa.tspresentation.test.jpa;

import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.jpa.SimpleJPABean;
import org.jboss.qa.tspresentation.utils.JdbcBean;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
public class JtaDatasourceTestCase {
    private static final Logger log = LoggerFactory.getLogger(JtaDatasourceTestCase.class);
    private static final String DEPLOYMENT = "jta-datasource";

    @Inject SimpleJPABean simpleJpaBean;

    @Inject JdbcBean jdbcBean;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.jpa")
                .addPackage("org.jboss.qa.tspresentation.utils") // utilities to do sql queries
                .addClass(ProjectProperties.class); // constant definitions
        jar.addAsManifestResource("ds-persistence.xml", "persistence.xml"); // src/test/resources
        return jar;
    }

    @Test
    public void test() throws SQLException {
        log.info("Running test with name '{}'", "my-testing-name");
        int id = simpleJpaBean.doPersist("my-testing-name");

        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals("my-testing-name", name);
    }
}
