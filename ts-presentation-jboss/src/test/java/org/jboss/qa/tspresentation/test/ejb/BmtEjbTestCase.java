package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.BmtBean;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
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

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
public class BmtEjbTestCase {
    private static final Logger log = LoggerFactory.getLogger(BmtEjbTestCase.class);
    private static final String DEPLOYMENT = "ejb-bmt-on-exception";

    @EJB BmtBean bmtBean;

    @EJB ResultsBean results;

    @PersistenceContext EntityManager em;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils").addClass(ProjectProperties.class)
                .addClass(BmtBean.class)
                .addClass(JBossTestEntity.class)
                .addAsManifestResource("beans.xml")
                .addAsManifestResource("jta-ds-persistence.xml", "persistence.xml");
        return jar;
    }

    @Before
    public void setUp() throws Exception {
        Context jndiCtx = new InitialContext();
        txManager = (TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager");
        results.clear();

        if(txManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            txManager.rollback();
        }

    }

    @Test
    public void runtimeExceptionTrown() throws Exception {
        try {
            bmtBean.bmtHasToEndTransaction();
            Assert.fail("Expecting exception");
        } catch (EJBException e) {
            // ignore
            // javax.ejb.EJBException: JBAS014581: EJB 3.1 FR 13.3.3: BMT bean ExceptionWorkerBmtEjbBean should complete transaction before returning.
        }

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getFirst().getCode());
    }
}