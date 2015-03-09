package org.jboss.qa.tspresentation.ejb;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.TxnDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class StatefulBmtSynchronizationAnnotationsBean {
    private static final Logger log = LoggerFactory.getLogger(StatefulBmtSynchronizationAnnotationsBean.class);

    @EJB
    private ResultsBean results;

    @Resource
    private UserTransaction utx;

    public void doWorkBmt() throws Exception {
        utx.begin();
        utx.commit();
    }

    @Remove
    public void remove() {
        // just hook to release sfsb
    }

    @AfterBegin
    public void afterBegin() throws EJBException, RemoteException {
        log.info("afterBegin");
        results.addStorageValue("afterBegin", new TxnDTO(getTransaction()));
    }

    @BeforeCompletion
    public void beforeCompletion() throws EJBException, RemoteException {
        log.info("beforeCompletion");
        results.addStorageValue("beforeCompletion", new TxnDTO(getTransaction()));
    }

    @AfterCompletion
    public void afterCompletion(final boolean committed) throws EJBException, RemoteException {
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
