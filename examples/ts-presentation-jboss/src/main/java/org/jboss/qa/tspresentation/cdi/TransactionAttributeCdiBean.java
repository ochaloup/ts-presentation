package org.jboss.qa.tspresentation.cdi;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.jboss.qa.tspresentation.utils.ResultsLogged;

@ResultsLogged
public class TransactionAttributeCdiBean {

    @Transactional(value = TxType.REQUIRED)
    public void required() {
    }

    @Transactional(value = TxType.NEVER)
    public void never() {
    }

    @Transactional(value = TxType.MANDATORY)
    public void mandatory() {
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void requiresNew() {
    }

    @Transactional(value = TxType.NOT_SUPPORTED)
    public void notSupported() {
    }

    @Transactional(value = TxType.SUPPORTS)
    public void supports() {
    }
}
