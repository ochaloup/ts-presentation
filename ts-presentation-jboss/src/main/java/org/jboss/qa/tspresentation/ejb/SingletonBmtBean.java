package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
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
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class SingletonBmtBean {
    private static final Logger log = LoggerFactory.getLogger(SingletonBmtBean.class);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @Resource
    private UserTransaction utx;

    /**
     * This is not permitted for Singleton bean to not finish transaction
     */
    public void beginTransaction() throws Exception {
        log.info("Beginning transation");
        utx.begin();

        JBossTestEntity entity = new JBossTestEntity("stateful-bmt-begin");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());
    }
}
