package org.jboss.qa.tspresentation.ejb.exception;

import javax.ejb.Stateless;

import org.jboss.qa.tspresentation.exception.ApplicationException;
import org.jboss.qa.tspresentation.exception.ApplicationRollbackException;
import org.jboss.qa.tspresentation.exception.RuntimeNoRollbackException;

@Stateless
public class ExceptionThrowerEjb {
    public void throwRuntimeException() {
        throw new RuntimeException();
    }

    public void throwRuntimeNotRollbackException() {
        throw new RuntimeNoRollbackException();
    }

    public void throwApplicationException() throws ApplicationException {
        throw new ApplicationException();
    }

    public void throwApplicationRollbackException() throws ApplicationRollbackException {
        throw new ApplicationRollbackException();
    }
}
