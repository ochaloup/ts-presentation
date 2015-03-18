package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Stateless
public class BeanSynchronization {
    private static final Logger log = LoggerFactory.getLogger(BeanSynchronization.class);

    @Inject
    private ResultsBean results;

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager tm;

    // As this is CMT bean we can't inject user transaction
    // user transaction could be injected only for BMT
    // @Resource
    // private UserTransaction utx;

    public void synchronize() throws IllegalStateException, RollbackException, SystemException {
        log.info("start method synchronize()");
        tm.getTransaction().registerSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                log.info("beforeCompletion()");
                results.addStorageValue("before", true);
            }
            @Override
            public void afterCompletion(final int status) {
                log.info("afterCompletion()");
                results.addStorageValue("after", status);
            }
        });
    }
}
