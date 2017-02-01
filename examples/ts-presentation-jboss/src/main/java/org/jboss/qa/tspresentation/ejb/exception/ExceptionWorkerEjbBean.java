package org.jboss.qa.tspresentation.ejb.exception;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.qa.tspresentation.exception.ApplicationException;
import org.jboss.qa.tspresentation.exception.ApplicationRollbackException;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;

@Stateless
@ResultsLogged
public class ExceptionWorkerEjbBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @EJB
    private ExceptionThrowerEjb exceptionThrower;

    public int doNotRollback() {
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
}
