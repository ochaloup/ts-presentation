package org.jboss.qa.tspresentation.bean;

import javax.ejb.ApplicationException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;

@Stateless
@TransactionManagement
public class TestBean {

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void doWork() {
        // do something
    }
}
