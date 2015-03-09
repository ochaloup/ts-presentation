package org.jboss.qa.tspresentation.test.ejb;


import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.ejb.TransactionAttributeBean;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for being able to check that container is started and that arquillian extension
 * was triggered.
 */
@RunWith(Arquillian.class)
public class TransactionAttributesTestCase {
    private static final Logger log = LoggerFactory.getLogger(TransactionAttributesTestCase.class);

    @EJB TransactionAttributeBean txnBean;

    @EJB ResultsBean results;

    TransactionManager txManager;

    @Deployment(name = "basic")
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic.jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(TransactionAttributeBean.class)
                .addAsManifestResource("beans.xml");
        return jar;
    }

    @Before
    public void setUp() throws NamingException {
        Context jndiCtx = new InitialContext();
        txManager = (TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager");
        results.clear();
    }

    @Test
    public void required() throws Exception {
        txnBean.required();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    @Test
    public void requiredStarted() throws Exception {
        txManager.begin();
        txnBean.required();
        TxnDTO txn = new TxnDTO(txManager.getTransaction());
        txManager.commit();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertEquals(txn, results.getTxnStatusAtInvoke().getFirst());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    @Test
    public void never() throws Exception {
        txnBean.never();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    /**
     * javax.ejb.EJBException: JBAS014163: Transaction present on server in Never call (EJB3 13.6.2.6)
     */
    @Test(expected = EJBException.class)
    public void neverStarted() throws Exception {
        try {
            txManager.begin();
            txnBean.never();
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
    }

    @Test
    public void requiresNew() throws Exception {
        txnBean.requiresNew();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    @Test
    public void requiresNewStarted() throws Exception {
        txManager.begin();
        txnBean.requiresNew();
        TxnDTO txn = new TxnDTO(txManager.getTransaction());
        txManager.commit();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_ACTIVE, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertNotEquals(txn, results.getTxnStatusAtInvoke().getFirst());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    // TODO: in case add tests for all attributes
}
