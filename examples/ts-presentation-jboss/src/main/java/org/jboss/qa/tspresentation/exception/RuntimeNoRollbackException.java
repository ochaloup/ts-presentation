package org.jboss.qa.tspresentation.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class RuntimeNoRollbackException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
