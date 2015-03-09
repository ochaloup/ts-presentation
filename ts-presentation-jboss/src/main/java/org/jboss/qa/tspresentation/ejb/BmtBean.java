package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;

@ResultsLogged
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @Resource
    private UserTransaction utx;

    public void bmtHasToEndTransaction() throws Exception {
        utx.begin();
    }
}
