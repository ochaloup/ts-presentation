package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Asynchronous
@Stateless
public class AsynchronouslyCalledBean {
    private static final Logger log = LoggerFactory.getLogger(AsynchronouslyCalledBean.class);

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager tm;

    public void beingCalled() {
        try {
            log.info("Was called having transaction status {} and id {}", tm.getStatus(), tm.getTransaction());
            log.info("Waiting for 1 s");
            Thread.sleep(1000);
            Thread.yield();
            log.info("#2 transaction status {} and id {}", tm.getStatus(), tm.getTransaction());
            log.info("Waiting for 1 s");
            Thread.sleep(1000);
            Thread.yield();
            log.info("#3 transaction status {} and id {}", tm.getStatus(), tm.getTransaction());
            log.info("Waiting for 1 s");
            Thread.sleep(1000);
            Thread.yield();
            log.info("#4 status {} and id {}", tm.getStatus(), tm.getTransaction());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
