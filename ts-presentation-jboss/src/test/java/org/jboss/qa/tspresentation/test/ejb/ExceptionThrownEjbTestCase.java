package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.exception.ExceptionWorkerEjbBean;
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

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
public class ExceptionThrownEjbTestCase {
    private static final String DEPLOYMENT = "ejb-rollback-on-exception";

    @EJB ExceptionWorkerEjbBean ejbBean;

    @EJB ResultsBean results;

    @PersistenceContext EntityManager em;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addPackage("org.jboss.qa.tspresentation.ejb.exception")
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
        int id = ejbBean.doNotRollback();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }

    @Test
    public void rollbackRuntimeException() throws Exception {
        int id = ejbBean.doRollbackRuntimeException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        Assert.assertNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, results.getTxnStatusAtExit().getLast().getCode());
    }

    @Test
    public void rollbackApplicationException() throws Exception {
        int id = ejbBean.doRollbackApplicationException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        Assert.assertNull(em.find(JBossTestEntity.class, id));

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, results.getTxnStatusAtExit().getLast().getCode());
    }

    @Test
    public void commitApplicationException() throws Exception {
        int id = ejbBean.doCommitApplicationException();

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
        int id = ejbBean.doCommitRuntimeException();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        JBossTestEntity entity = em.find(JBossTestEntity.class, id);
        Assert.assertEquals(id, entity.getId());

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getLast().getCode());
    }
}