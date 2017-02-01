package org.jboss.qa.tspresentation.ejb.exception;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResultsLogged
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ExceptionWorkerBmtEjbBean {
    private static final Logger log = LoggerFactory.getLogger(ExceptionWorkerBmtEjbBean.class);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @EJB
    private ExceptionThrowerEjb exceptionThrower;

    @Resource
    private UserTransaction utx;

    @Resource
    private SessionContext ctx;

    public void runtimeExceptionThrown() throws Exception {
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("some-name");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException re) {
            // ignore
        }

        try {
            utx.commit();
        } catch (Exception e) {
            // utx.commit won't succeed as there was RuntimeException thrown before
            // but we can't run utx.rollback either as it fails of not active txn
            utx.rollback();
        }
    }

    public void runtimeExceptionThrownAutomaticRollback() throws Exception {
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("some-name");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException re) {
            // ignore
        }
    }

    /**
     * TODO: is this against spec as setRollbackOnly is available for BMT?
     * 8.3.3.1: An enterprise bean with bean-managed transaction demarcation must not use the getRollbackOnly
     * and setRollbackOnly methods of the EJBContext interface.
     */
    public void setRollbackOnly() throws Exception {
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("some-name");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException re) {
            utx.setRollbackOnly();
        }

        try {
            utx.commit();
        } catch (Exception e) {
            // utx.commit won't succeed as there was RuntimeException thrown before
            // but we can't run utx.rollback either as it fails of not active txn
            log.info("E: {}", e); // Caused by: java.lang.Throwable: setRollbackOnly called from ...
            utx.rollback();
        }
    }

    public void setRollbackOnlyOnSessionContext() throws Exception {
        UserTransaction ctxUtx = ctx.getUserTransaction();
        ctxUtx.begin();

        JBossTestEntity entity = new JBossTestEntity("some-name");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());

        try {
            exceptionThrower.throwRuntimeException();
        } catch (RuntimeException re) {
            ctxUtx.setRollbackOnly();
        }

        try {
            ctxUtx.commit();
        } catch (Exception e) {
            // utx.commit won't succeed as there was RuntimeException thrown before
            // but we can't run utx.rollback either as it fails of not active txn
            log.info("E: {}", e); // Caused by: java.lang.Throwable: setRollbackOnly called from ...
            ctxUtx.rollback();
        }
    }
}
