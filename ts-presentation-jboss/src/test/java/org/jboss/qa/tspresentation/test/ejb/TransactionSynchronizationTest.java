package org.jboss.qa.tspresentation.test.ejb;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BeanSynchronization;
import org.jboss.qa.tspresentation.ejb.BeanSynchronizationRegistry;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
public class TransactionSynchronizationTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionSynchronizationTest.class);
    private static final String DEPLOYMENT = "synchronization-registry";

    @EJB
    private BeanSynchronizationRegistry beanRegistry;

    @EJB
    private BeanSynchronization beanSynchronization;

    @EJB
    private ResultsBean results;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(BeanSynchronizationRegistry.class)
                .addClass(BeanSynchronization.class);
        return jar;
    }

    @Before
    public void setUp() {
        results.clear();
    }

    @Test
    public void testRegistry() {
        log.info("test testRegistry");
        beanRegistry.synchronize();

        Assert.assertNotNull(results.getStorageValue("before"));
        Assert.assertNotNull(results.getStorageValue("after"));
    }

    @Test
    public void testSynchronization() throws Exception {
        log.info("test testSynchronization");
        beanSynchronization.synchronize();

        Assert.assertNotNull(results.getStorageValue("before"));
        Assert.assertNotNull(results.getStorageValue("after"));
    }
}
