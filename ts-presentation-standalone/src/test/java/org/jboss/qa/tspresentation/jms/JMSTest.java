package org.jboss.qa.tspresentation.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Assert;
import org.junit.Test;

public class JMSTest {
    @Test
    public void test() throws JMSException {
        String text = "text";
        Connection jmsConnection = JMSProvider.getConnection();
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        JMSProvider.sendMessage(text, session);

        Assert.assertEquals(text, JMSProvider.receiveMessage(jmsConnection, session));
    }

}
