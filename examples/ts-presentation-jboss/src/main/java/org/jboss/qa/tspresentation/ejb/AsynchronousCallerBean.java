package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class AsynchronousCallerBean {
    private static final Logger log = LoggerFactory.getLogger(AsynchronousCallerBean.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @EJB
    private AsynchronouslyCalledBean bean;

    public void callAsync() {
        try {
            log.info("Calling bean {} transaction status {} and id {}", bean, tm.getStatus(), tm.getTransaction());
            bean.beingCalled();
            log.info("Waiting for 2 s");
            Thread.sleep(2000);
            Thread.yield();
            log.info("#2 transaction status {} and id {}", tm.getStatus(), tm.getTransaction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
