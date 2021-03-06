package org.jboss.qa.tspresentation.dummy;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@TransactionManagement
public class DummyBean {
    private static final Logger log = LoggerFactory.getLogger(DummyBean.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void doWork() {
        // do something
        log.info("I'm here!");
    }
}
