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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResultsLogged
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessBmtBean {
    private static final Logger log = LoggerFactory.getLogger(StatelessBmtBean.class);

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

    public void setTimeoutWrong() throws Exception {
        try {
            utx.begin();
            utx.setTransactionTimeout(1);
            Thread.sleep(2000);

            JBossTestEntity entity = new JBossTestEntity("stateless-bmt-begin");
            em.persist(entity);
            results.addStorageValue("id", entity.getId());

            utx.commit();
        } catch (Exception e) {
            utx.rollback();
        }
    }

    public void setTimeoutCorrect() throws Exception {
        try {
            utx.setTransactionTimeout(1);
            utx.begin();
            Thread.sleep(2000);

            JBossTestEntity entity = new JBossTestEntity("stateless-bmt-begin");
            em.persist(entity);
            // transaction will timeouted so the getting id will fail
            results.addStorageValue("id", entity.getId());

            utx.commit();
            log.info("Timeouted transaction was committed");
        } catch (Exception e) {
            log.info("Timeouted transaction was rollbacked");
            utx.rollback();
        }
    }
}
