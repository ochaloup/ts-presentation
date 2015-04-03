package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.AsynchronousCallerBean;
import org.jboss.qa.tspresentation.ejb.AsynchronouslyCalledBean;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(Arquillian.class)
public class AsynchronousCallTestCase {
    private static final Logger log = LoggerFactory.getLogger(AsynchronousCallTestCase.class);
    private static final String DEPLOYMENT = "asynchronous-call-example";

    @EJB AsynchronousCallerBean bean;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils").addClass(ProjectProperties.class)
                .addClass(AsynchronousCallerBean.class)
                .addClass(AsynchronouslyCalledBean.class);
        return jar;
    }

    @Before
    public void setUp() throws Exception {
        Context jndiCtx = new InitialContext();
        txManager = (TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager");

        if(txManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            txManager.rollback();
        }

    }

    /**
     * One of tests where the functionality is not check in any way but it's a check how
     * things works. You will ned to consult server log.
     *
     * This is check that transaction context is *not* propagated when asynchronous invocation
     * is used (EJB 4.5.3)
     */
    @Test
    public void statelessNotFinishedTransaction() throws Exception {
        bean.callAsync();
    }
}