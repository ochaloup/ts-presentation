package org.jboss.qa.tspresentation.ejb;

import java.rmi.RemoteException;

import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.TxnDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateful
public class StatefulSynchronizationAnnotationsBean {
    private static final Logger log = LoggerFactory.getLogger(StatefulSynchronizationAnnotationsBean.class);

    @EJB
    private ResultsBean results;

    public void doWorkSynchro() {
        // not much to do in fact
    }

    @Remove
    public void remove() {
        // just hook to release sfsb
    }

    @AfterBegin
    private void afterBegin() throws EJBException, RemoteException {
        log.info("afterBegin");
        results.addStorageValue("afterBegin", new TxnDTO(getTransaction()));
    }

    @BeforeCompletion
    private void beforeCompletion() throws EJBException, RemoteException {
        log.info("beforeCompletion");
        results.addStorageValue("beforeCompletion", new TxnDTO(getTransaction()));
    }

    @AfterCompletion
    private void afterCompletion(final boolean committed) throws EJBException, RemoteException {
        log.info("afterCompletion");
        results.addStorageValue("afterCompletion", new TxnDTO(getTransaction()));
        results.addStorageValue("committed", committed);
    }

    private Transaction getTransaction() {
        try {
            Context jndiCtx = new InitialContext();
            return ((TransactionManager) jndiCtx.lookup("java:jboss/TransactionManager")).getTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
