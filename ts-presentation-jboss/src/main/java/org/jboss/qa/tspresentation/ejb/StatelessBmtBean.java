package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;

@ResultsLogged
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessBmtBean {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @Resource
    private UserTransaction utx;

    /**
     * Not permitted as SLSB has to finish transaction before
     * end of method
     */
    public void beginTransaction() throws Exception {
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("stateless-bmt-begin");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());
    }
}
