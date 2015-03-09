package org.jboss.qa.tspresentation.test.cdi;


import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionalException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.qa.tspresentation.cdi.TransactionAttributeCdiBean;
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
public class TransactionAttributesCdiTestCase {
    private static final Logger log = LoggerFactory.getLogger(TransactionAttributesCdiTestCase.class);

    @Inject TransactionAttributeCdiBean cdiBean;

    @EJB ResultsBean results;

    TransactionManager txManager;

    @Deployment(name = "basic")
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic.jar")
                .addPackage("org.jboss.qa.tspresentation.utils")
                .addClass(ProjectProperties.class)
                .addClass(TransactionAttributeCdiBean.class)
                .addAsManifestResource("beans.xml");
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
    public void required() throws Exception {
        cdiBean.required();

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
        cdiBean.required();
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
        cdiBean.never();

        Assert.assertEquals(1, results.getTxnStatusAtInvoke().size());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, results.getTxnStatusAtInvoke().getFirst().getCode());
        Assert.assertEquals(1, results.getTxnStatusAtExit().size());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, results.getTxnStatusAtExit().getFirst().getCode());
        Assert.assertEquals(results.getTxnStatusAtInvoke().getFirst(), results.getTxnStatusAtExit().getFirst());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        log.info("Transaction statuses - invoke: {} and exit: {}", results.getTxnStatusAtInvoke(), results.getTxnStatusAtExit());
    }

    @Test(expected = TransactionalException.class)
    public void neverStarted() throws Exception {
        try {
            txManager.begin();
            cdiBean.never();
        } catch (Exception e) {
            txManager.rollback();
            throw e;
        }
        Assert.fail("Expected that this throws exception as transaction was started by test");
    }

    @Test
    public void requiresNew() throws Exception {
        cdiBean.requiresNew();

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
        cdiBean.requiresNew();
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

    @Test(expected = TransactionalException.class)
    public void mandatory() throws Exception {
        cdiBean.mandatory();
        Assert.fail("Expected that this throws exception as transaction was started by test");
    }

    // TODO: in case add tests for all attributes
}
