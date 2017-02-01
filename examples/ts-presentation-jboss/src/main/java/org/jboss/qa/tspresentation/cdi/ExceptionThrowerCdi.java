package org.jboss.qa.tspresentation.cdi;

import org.jboss.qa.tspresentation.exception.ApplicationException;
import org.jboss.qa.tspresentation.exception.ApplicationRollbackException;
import org.jboss.qa.tspresentation.exception.RuntimeNoRollbackException;

public class ExceptionThrowerCdi {
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
