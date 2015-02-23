package org.jboss.qa.tspresentation.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMSTest {
    private static final Logger log = LoggerFactory.getLogger(JMSTest.class);

    private static final String MESSAGE = "One Ring to rule them all";
    private Connection jmsConnection;

    @Before
    public void setUp() throws JMSException {
        jmsConnection = JMSProvider.getConnection();
    }

    @After
    public void tearDown() throws JMSException {
        jmsConnection.close();
    }

    @Test
    public void simpleSendWithReceive() throws JMSException {
        log.debug("Sending message {} to through connection {}", MESSAGE, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        JMSProvider.sendMessage(MESSAGE, session);

        Assert.assertEquals(MESSAGE, JMSProvider.receiveMessage(jmsConnection, session));
    }

}
