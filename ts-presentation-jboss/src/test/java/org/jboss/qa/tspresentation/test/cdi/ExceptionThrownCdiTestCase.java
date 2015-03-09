package org.jboss.qa.tspresentation.test.cdi;


import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.cdi.ExceptionWorkerCdiBean;
import org.jboss.qa.tspresentation.exception.ApplicationRollbackException;
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

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
public class ExceptionThrownCdiTestCase {
    private static final Logger log = LoggerFactory.getLogger(ExceptionThrownCdiTestCase.class);
    private static final String DEPLOYMENT = "cdi-rollback-on-exception";

    @Inject ExceptionWorkerCdiBean cdiBean;

    @EJB ResultsBean results;

    @PersistenceContext EntityManager em;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addPackage("org.jboss.qa.tspresentation.cdi")
                .addPackage("org.jboss.qa.tspresentation.exception")
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
    public void goingToCommit() throws Exception {
        int id = cdiBean.doNotRollback();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }

    /**
     * RuntimeException thrown from CDI bean does not mark for rollback - TODO: against spec?
     */
    @Ignore
    @Test
    public void rollbackOnCdiBeanNotMarkToRollback() throws Exception {
        int id = cdiBean.doRollbackRuntimeException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        Assert.assertNull(em.find(JBossTestEntity.class, id));

        /*
        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, results.getTxnStatusAtExit().getLast().getCode());
        */
    }

    @Test
    public void rollbackRuntimeExceptionThrownToApp() throws Exception {
        try {
            cdiBean.doRollbackRuntimeExceptionThrownToApp();
            Assert.fail("Expecting runtime exception");
        } catch (RuntimeException re) {
            // ignore
        }

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        int id = (Integer) results.getStorageValue("id");
        log.info("Searching for entity with id {}", id);
        Assert.assertNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(0, results.getTxnStatusAtExit().size());
    }

    /**
     * Is this against spec?
     */
    @Ignore
    @Test
    public void rollbackApplicationException() throws Exception {
        int id = cdiBean.doRollbackApplicationException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        Assert.assertNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, results.getTxnStatusAtExit().getLast().getCode());
    }

    /**
     * ApplicationException(rollback = true) is not reflected by CDI
     */
    @Test
    public void rollbackApplicationExceptionThrownToApp() throws Exception {
        try {
            cdiBean.doRollbackApplicationExceptionThrowToApp();
            Assert.fail("Exception thrown is expected");
        } catch (ApplicationRollbackException e) {
            // ignore
        }

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        int id = (Integer) results.getStorageValue("id");
        log.info("Searching for entity with id {}", id);
        Assert.assertNotNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(0, results.getTxnStatusAtExit().size());
    }

    @Test
    public void commitApplicationException() throws Exception {
        int id = cdiBean.doCommitApplicationException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }

    @Test
    public void commitRuntimeException() throws Exception {
        int id = cdiBean.doCommitRuntimeException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }

    @Test
    public void commitRuntimeExceptionDontRollbackParam() throws Exception {
        int id = cdiBean.doCommitRuntimeExceptionDontRollbackParam();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }

    /**
     * Not sure if this is against spec or not?
     */
    @Ignore
    @Test
    public void rollbackApplicationExceptionDoRollbackParam() throws Exception {
        int id = cdiBean.doRollbackApplicationExceptionDoRollbackParam();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        Assert.assertNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, results.getTxnStatusAtExit().getLast().getCode());
    }
}