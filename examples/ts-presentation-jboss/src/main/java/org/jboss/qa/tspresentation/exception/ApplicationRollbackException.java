package org.jboss.qa.tspresentation.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ApplicationRollbackException extends Exception {
    private static final long serialVersionUID = 1L;
}
