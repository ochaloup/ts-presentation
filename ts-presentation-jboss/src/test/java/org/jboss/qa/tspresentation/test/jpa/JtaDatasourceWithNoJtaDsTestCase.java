package org.jboss.qa.tspresentation.test.jpa;

import java.io.IOException;
import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
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
public class JtaDatasourceWithNoJtaDsTestCase {
    private static final Logger log = LoggerFactory.getLogger(JtaDatasourceWithNoJtaDsTestCase.class);
    private static final String DEPLOYMENT = "jta-datasource-nonjta";

    ModelControllerClient controllerClient = null;

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
    public void baseTest() throws SQLException {
        int id = simpleJpaBean.doPersist("abc");

        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals("my-testing-name", name);
    }

    @Test
    public void datasourceJtaFalse() throws SQLException, IOException {
        log.error("Fuck you!!!");
        simpleJpaBean.doPersist("aaaaaaaaaaaaaa");
        /*int id = simpleJpaBean.doPersist("my-testing-name");

        String name = jdbcBean.getSingle(JBossTestEntity.TABLE_NAME, id, JBossTestEntity.NAME_COLUMN_NAME);

        Assert.assertEquals("my-testing-name", name);*/
        ModelNode mn = new ModelNode();
        mn.get(ModelDescriptionConstants.OP).set("read-resource");
        mn.get(ModelDescriptionConstants.OP_ADDR).set(new ModelNode());
        log.info("output is: {}", controllerClient.execute(mn));
    }
}
