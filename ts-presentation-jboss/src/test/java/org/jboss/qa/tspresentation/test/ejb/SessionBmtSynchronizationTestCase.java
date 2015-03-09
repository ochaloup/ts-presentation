package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.StatefulBmtSynchronizationAnnotationsBean;
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
public class SessionBmtSynchronizationTestCase {
    private static final String DEPLOYMENT_BMT_STATEFUL = "ejb-stateless-bmt-synchro";

    @EJB StatefulBmtSynchronizationAnnotationsBean statefulBmtSynchro;

    @EJB ResultsBean results;

    TransactionManager txManager;

    @Deployment(name = DEPLOYMENT_BMT_STATEFUL)
    public static Archive<?> deployBmtSynchro() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_BMT_STATEFUL + ".jar")
                .addPackage("org.jboss.qa.tspresentation.utils").addClass(ProjectProperties.class)
                .addClass(StatefulBmtSynchronizationAnnotationsBean.class)
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

    /**
     * As bean manages the transaction on its own the context synchronization callbacks
     * are not called (are ignored).
     */
    @Test
    public void statefulBmtSynchro() throws Exception {
        statefulBmtSynchro.doWorkBmt();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        TxnDTO txn = getTxn("afterBegin");
        Assert.assertNull(txn);
        txn = getTxn("beforeCompletion");
        Assert.assertNull(txn);
        txn = getTxn("afterCompletion");
        Assert.assertNull(txn);

        statefulBmtSynchro.remove();
    }

    private TxnDTO getTxn(final String key) {
        return (TxnDTO) results.getStorageValue(key);
    }
}