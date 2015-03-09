package org.jboss.qa.tspresentation.ejb;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.qa.tspresentation.utils.ResultsLogged;

@Stateless
@ResultsLogged
public class TransactionAttributeEjbBean {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void required() {
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void never() {
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void mandatory() {
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void requiresNew() {
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void notSupported() {
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void supports() {
    }
}
