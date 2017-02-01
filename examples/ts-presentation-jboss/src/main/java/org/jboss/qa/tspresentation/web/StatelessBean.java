package org.jboss.qa.tspresentation.web;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class StatelessBean {
    private static final Logger log = LoggerFactory.getLogger(WebServletStatefulBeanByInject.class);

    public void doSomeWork() {
        log.info("Doing some work which takes 5 seconds");
        for (int i = 1; i<=5; i++) {
            try {
                log.info("Working " + i);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore - don't do this :)
            }
        }
        log.info("Work done");
    }
}
