package org.jboss.qa.tspresentation.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMSTest {
    private static final Logger log = LoggerFactory.getLogger(JMSTest.class);

    private static final String BASE_MESSAGE = " rules them all";
    private String message;
    private Connection jmsConnection;

    @Rule public TestName testName = new TestName();

    @Before
    public void setUp() throws JMSException {
        message = testName + BASE_MESSAGE;
        jmsConnection = JMSProvider.getConnection();

        // cleaning queue
        Session session = null;
        try {
            while (true) {
                session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                String msg = JMSProvider.receiveMessageAsString(jmsConnection, session);
                log.info("Cleanign queue - received '" + msg + "'");
                session.close();
            }
        } catch (NullPointerException npe) {
            // ignore as it's expected
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }

    @After
    public void tearDown() throws JMSException {
        jmsConnection.close();
    }

    @Test
    public void sessionWithAutoAck() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        JMSProvider.sendMessage(message, session);

        Assert.assertEquals(message, JMSProvider.receiveMessageAsString(jmsConnection, session));

        // for full acknowledgement the session has to be closed
        session.close();
        // there should not be any message
        checkIsNull(jmsConnection);
    }

    // TODO: create some more reasonable test :)
    @Test
    public void sessionWithDuplAck() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);

        JMSProvider.sendMessage(message, session);

        Assert.assertEquals(message, JMSProvider.receiveMessageAsString(jmsConnection, session));
        session.close();
    }

    @Test
    public void sessionWithClientAck() throws JMSException, InterruptedException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        JMSProvider.sendMessage(message, session);

        TextMessage textMessage = JMSProvider.receiveMessage(jmsConnection, session);
        Assert.assertEquals(message, textMessage.getText());

        jmsConnection.close();
        jmsConnection = JMSProvider.getConnection();
        session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        textMessage = JMSProvider.receiveMessage(jmsConnection, session);
        Assert.assertEquals(message, textMessage.getText());

        // we are acknowledging the receive of the message and the jms server will forget about the message
        textMessage.acknowledge();

        jmsConnection.close();
        jmsConnection = JMSProvider.getConnection();

        checkIsNull(jmsConnection);
    }

    @Test
    public void simpleTransacted() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

        JMSProvider.sendMessage(message, session);

        // message was sent but not commited so no message should be delivered back
        checkIsNull(jmsConnection);

        session.commit();

        Assert.assertEquals(message, JMSProvider.receiveMessageAsString(jmsConnection, session));
        session.commit();

        checkIsNull(jmsConnection);
    }

    /**
     * Verifying that there is no other message in queue
     * Using already created connection
     */
    private void checkIsNull(final Connection conn) throws JMSException {
        Session session = null;
        try {
            session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            TextMessage textMessage = JMSProvider.receiveMessage(conn, session);
            Assert.fail("Expecting that message was acknowledge and server deleted it from its logs but we received " + textMessage
                    + " with text '" + textMessage.getText() + "'");
        } catch (NullPointerException npe) {
            // there is no message on the jms server - that's ok
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }
}
