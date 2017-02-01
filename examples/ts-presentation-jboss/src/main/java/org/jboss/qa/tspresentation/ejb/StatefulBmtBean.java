package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.jboss.qa.tspresentation.utils.ResultsLogged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResultsLogged
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class StatefulBmtBean {
    private static final Logger log = LoggerFactory.getLogger(StatefulBmtBean.class);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @Resource
    private UserTransaction utx;

    /**
     * This is permitted as SFSB can transfer a transaction over
     * several method invocations
     */
    public void beginTransaction() throws Exception {
        log.info("Beginning transation");
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("stateful-bmt-begin");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());
    }

    public void commitTransaction() throws Exception {
        log.info("Commiting transation");
        utx.commit();
    }

    @Remove
    public void remove() {
        log.info("Stateful bean remove callback");
    }
}
