package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.StatefulSynchronizationAnnotationsBean;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.TxnDTO;
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
public class SessionSynchronizationTestCase {
    private static final String DEPLOYMENT_STATEFUL = "ejb-stateful-synchro";

    @EJB StatefulSynchronizationAnnotationsBean statefulSynchro;

    @EJB ResultsBean results;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT_STATEFUL)
    public static Archive<?> deploySynchro() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_STATEFUL + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils").addClass(ProjectProperties.class)
                .addClass(StatefulSynchronizationAnnotationsBean.class)
                .addClass(JBossTestEntity.class);
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
    public void statefulSynchro() throws Exception {
        statefulSynchro.doWorkSynchro();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        TxnDTO txn = getTxn("afterBegin");
        Assert.assertEquals(Status.STATUS_ACTIVE, txn.getCode());
        txn = getTxn("beforeCompletion");
        Assert.assertEquals(Status.STATUS_ACTIVE, txn.getCode());
        txn = getTxn("afterCompletion");
        Assert.assertEquals(Status.STATUS_COMMITTED, txn.getCode());
        boolean committed = (Boolean) results.getStorageValue("committed");
        Assert.assertEquals(true, committed);

        statefulSynchro.remove();
    }

    private TxnDTO getTxn(final String key) {
        return (TxnDTO) results.getStorageValue(key);
    }
}