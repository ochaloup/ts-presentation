package org.jboss.qa.tspresentation.web;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;

/**
 * Shamelessly taken from http://www.adam-bien.com/roller/abien/entry/does_cdi_injection_of_sessionscoped
 */
@Stateful
@SessionScoped
public class SessionStore implements Serializable {
    private static final long serialVersionUID = 1L;

    public static AtomicLong INSTANCE_COUNT = new AtomicLong(0);

    private String payload;

    @PostConstruct
    public void onNewSession(){
        INSTANCE_COUNT.incrementAndGet();
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    @PreDestroy
    public void onSessionDestruction(){
        INSTANCE_COUNT.decrementAndGet();
    }

}