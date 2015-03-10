package org.jboss.qa.tspresentation.ejb;

import javax.ejb.Stateless;


@Stateless
public class TransactionAttributeEjbChildBean extends TransactionAttributeEjbBean {

    @Override
    public void never() {
    }
}
