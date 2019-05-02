package org.jboss.qa.tspresentation.cdi;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.exception.ApplicationException;
import org.jboss.qa.tspresentation.exception.ApplicationRollbackException;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;

@Transactional
@ResultsLogged
public class ExceptionWorkerCdiBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @Inject
    private ExceptionThrowerCdi exceptionThrower;

    /*
     * Injection of the 'utx' is permitted in @Transactional CDI bean.
     * But if the 'utx' is called in the @Transactional method
     * then IllegalStateException is thrown. E.g. on 'utx.getStatus() he stacktrace is:
     *   at org.wildfly.transaction.client.LocalUserTransaction.checkTransactionStateAvailability(LocalUserTransaction.java:112)
     *   at org.wildfly.transaction.client.LocalUserTransaction.getStatus(LocalUserTransaction.java:67)
     *   at org.jboss.qa.tspresentation.cdi.ExceptionWorkerCdiBean.doNotRollback(ExceptionWorkerCdiBean.java:36)
     *   ...
     * Note: java:/comp/UserTransaction works only in EJB (as it's component namespace)
     *       for lookup  in CDI it's needed to be used global JNDI name.
     */
    /* @Resource(lookup = "java:jboss/UserTransaction")
    private UserTransaction utx; */

    @Resource(lookup = "java:/TransactionManager")
    TransactionManager tm;

    @Transactional(value = TxType.REQUIRES_NEW)
    public int doNotRollback() {
    	try {
			System.out.println(">>>> " + tm.getTransaction().toString());
		} catch (SystemException e1) {
			e1.printStackTrace();
		}
        JBossTestEntity entity = new JBossTestEntity("some-name");
        em.persist(entity);

        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            // ignoring
        }

        return entity.getId();
    }

    public int doRollbackRuntimeException() {
        JBossTestEntity entity = new JBossTestEntity("doRollbackRuntimeException");
        em.persist(entity);

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException e) {
            // ignoring
        }

        return entity.getId();
    }

    public void doRollbackRuntimeExceptionThrownToApp() {
        JBossTestEntity entity = new JBossTestEntity("doRollbackRuntimeException");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        exceptionThrower.throwRuntimeException();
    }

    public int doRollbackApplicationException() {
        JBossTestEntity entity = new JBossTestEntity("doRollbackApplicationException");
        em.persist(entity);

        try {
            exceptionThrower.throwApplicationRollbackException();
        } catch (ApplicationRollbackException e) {
            // ignoring
        }

        return entity.getId();
    }

    public void doRollbackApplicationExceptionThrowToApp() throws ApplicationRollbackException {
        JBossTestEntity entity = new JBossTestEntity("doRollbackApplicationException");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        exceptionThrower.throwApplicationRollbackException();
    }

    public int doCommitApplicationException() {
        JBossTestEntity entity = new JBossTestEntity("doCommitApplicationException");
        em.persist(entity);

        try {
            exceptionThrower.throwApplicationException();
        } catch (ApplicationException e) {
            // ignoring
        }

        return entity.getId();
    }

    public int doCommitRuntimeException() {
        JBossTestEntity entity = new JBossTestEntity("doCommitRuntimeException");
        em.persist(entity);

        try {
            exceptionThrower.throwRuntimeNotRollbackException();
        } catch (RuntimeException e) {
            // ignoring
        }

        return entity.getId();
    }

    @Transactional(dontRollbackOn = RuntimeException.class)
    public int doCommitRuntimeExceptionDontRollbackParam() {
        JBossTestEntity entity = new JBossTestEntity("doCommitRuntimeException2");
        em.persist(entity);

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException e) {
            // ignoring
        }

        return entity.getId();
    }

    @Transactional(rollbackOn = ApplicationException.class)
    public int doRollbackApplicationExceptionDoRollbackParam() {
        JBossTestEntity entity = new JBossTestEntity("doCommitRuntimeException2");
        em.persist(entity);

        try {
            exceptionThrower.throwApplicationException();
        } catch (ApplicationException e) {
            // ignoring
        }

        return entity.getId();
    }
}
