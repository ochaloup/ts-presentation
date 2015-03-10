package org.jboss.qa.tspresentation.ejb.failconnection;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expecting that em is configured to the same datasource
 * as the datasource used in TableCreator bean.
 */
@Stateless
public class CallerBean {
    private static final Logger log = LoggerFactory.getLogger(CallerBean.class);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ResultsBean results;

    @EJB
    private TableCreator tcBean;

    public void callRethrower() {
        try {
            tcBean.createTableWithRethrow();
        } catch (RuntimeException re) {
            // ignore
        }

        JBossTestEntity entity = new JBossTestEntity("rethrower");
        em.persist(entity); // there is no transaction at this time
        results.addStorageValue("id", entity.getId());
    }

    public void callCatch() {
        try {
            tcBean.createTableAndCatch();
        } catch (RuntimeException re) {
            // ignore
        }

        // org.postgresql.util.PSQLException: ERROR: current transaction is aborted, commands ignored until end of transaction block
        // connection is down for accepting data and so this will fail
        // the reason (as I understand) is that connection is reused for the datasource
        // and for the entity manager
        JBossTestEntity entity = new JBossTestEntity("catch");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());
    }

    public void callRequiresNew() {
        tcBean.createTableInNewTxn();

        JBossTestEntity entity = new JBossTestEntity("catch");
        em.persist(entity);
        results.addStorageValue("id", entity.getId());
    }
}
