package org.jboss.qa.tspresentation.test.ejb;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BeanSynchronizationRegistry;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
public class SynchronizationRegistryTest {
    private static final Logger log = LoggerFactory.getLogger(SynchronizationRegistryTest.class);
    private static final String DEPLOYMENT = "synchronization-registry";

    @EJB
    private BeanSynchronizationRegistry bean;

    @EJB
    private ResultsBean results;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(BeanSynchronizationRegistry.class);
        return jar;
    }

    @Test
    public void testRegistry() {
        log.info("test testRegistry");
        bean.synchronize();

        Assert.assertNotNull(results.getStorageValue("before"));
        Assert.assertNotNull(results.getStorageValue("after"));
    }
}
