package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class BeanSynchronizationRegistry {
    private static final Logger log = LoggerFactory.getLogger(BeanSynchronizationRegistry.class);

    @Inject
    private ResultsBean results;

    @Resource
    private TransactionSynchronizationRegistry synchroRegistry;

    public void synchronize() {
        log.info("start method synchronize()");
        synchroRegistry.registerInterposedSynchronization(new Synchronization() {
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
