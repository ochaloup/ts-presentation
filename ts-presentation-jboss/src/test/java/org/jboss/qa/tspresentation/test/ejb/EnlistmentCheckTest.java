package org.jboss.qa.tspresentation.test.ejb;

import javax.ejb.EJB;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BeanToCheckEnlistment;
import org.jboss.qa.tspresentation.ejb.BeanToCheckMessageDriven;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
public class EnlistmentCheckTest {
    private static final Logger log = LoggerFactory.getLogger(EnlistmentCheckTest.class);
    private static final String DEPLOYMENT = "enlistment-check";

    @EJB
    private BeanToCheckEnlistment bean;

    @EJB
    private ResultsBean results;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(BeanToCheckEnlistment.class)
                .addClass(BeanToCheckMessageDriven.class)
                .addClass(JBossTestEntity.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource("jta-ds-persistence.xml", "persistence.xml");
        return jar;
    }

    @Before
    public void setUp() {
        results.clear();
    }

    @Test
    public void notSupportedDb() {
        log.info("test notSupportedDb");
        bean.notSupportedDatabase();
    }

    @Test
    public void notSupportedJms() throws InterruptedException {
        log.info("test notSupportedJms");
        bean.notSupportedJms();
        Thread.sleep(2 * 1000);
        Assert.assertNotNull(results.getStorageValue("mdb"));
    }

    @Test
    public void requiresNewDb() {
        log.info("test requiresNewDb");
        bean.requiresNewDatabase();
    }

    @Test
    public void requiresNewJms() throws InterruptedException {
        log.info("test requiresNewJms");
        bean.requiresNewJms();
        Thread.sleep(2 * 1000);
        Assert.assertNotNull(results.getStorageValue("mdb"));
    }

    /**
     * By debug we can see what happens when jms connection is closed
     */
    @Test
    public void requiresNewJmsClose() throws InterruptedException {
        log.info("test requiresNewJms");
        bean.requiresNewJmsWithClose();
        Thread.sleep(2 * 1000);
        Assert.assertNotNull(results.getStorageValue("mdb"));
    }

    /**
     * Taking {@link UserTransaction} from JNDI is guarded by container
     * as well - so no possibility to take it.
     */
    @Ignore
    @Test
    public void useUserTransactionInCMT() throws Exception {
        log.info("test useUserTransactionInCMT");
        bean.useUserTransaction();
    }
}
