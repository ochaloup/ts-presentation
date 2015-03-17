package org.jboss.qa.tspresentation.jms;

import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_QUEUE;


import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.jboss.qa.tspresentation.utils.ProjectProperties;
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
        message = testName.getMethodName() + BASE_MESSAGE;
        jmsConnection = JmsProvider.getConnection();

        // cleaning queue
        Session session = null;
        try {
            while (true) {
                session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                String msg = JmsProvider.receiveMessageAsString(jmsConnection, session);
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

    /**
     * Base usage
     */
    @Test
    public void howTo() throws JMSException {
        try (Connection connection = jmsConnection = JmsProvider.getConnection()) {

            // AUTO_ACK
            Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue testQueue = session.createQueue(ProjectProperties.get(JMS_QUEUE));

            // for sending message I do not need to start connection, for receiving we need it
            MessageProducer producer = session.createProducer(testQueue);
            TextMessage msg = session.createTextMessage("hello there");
            producer.send(msg);

            session.close();

            // TRANSACTED
            session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

            testQueue = session.createQueue(ProjectProperties.get(JMS_QUEUE));

            connection.start();
            MessageConsumer consumer = session.createConsumer(testQueue);
            TextMessage received = (TextMessage) consumer.receive(3000); // 3s
            session.commit();
            connection.stop();

            Assert.assertEquals("hello there", received.getText());
        }
    }

    @Test
    public void sessionWithAutoAck() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        JmsProvider.sendMessage(message, session);

        Assert.assertEquals(message, JmsProvider.receiveMessageAsString(jmsConnection, session));

        // for full acknowledgement the session has to be closed
        session.close();
        // there should not be any message
        checkQueueIsEmpty(jmsConnection);
    }

    // TODO: create some more reasonable test :)
    @Test
    public void sessionWithDuplAck() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);

        JmsProvider.sendMessage(message, session);

        Assert.assertEquals(message, JmsProvider.receiveMessageAsString(jmsConnection, session));
        session.close();
    }

    @Test
    public void sessionWithClientAck() throws JMSException, InterruptedException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        JmsProvider.sendMessage(message, session);

        TextMessage textMessage = JmsProvider.receiveMessage(jmsConnection, session);
        Assert.assertEquals(message, textMessage.getText());

        jmsConnection.close();
        jmsConnection = JmsProvider.getConnection();
        session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        textMessage = JmsProvider.receiveMessage(jmsConnection, session);
        Assert.assertEquals(message, textMessage.getText());

        // we are acknowledging the receive of the message and the jms server will forget about the message
        textMessage.acknowledge();

        jmsConnection.close();
        jmsConnection = JmsProvider.getConnection();

        checkQueueIsEmpty(jmsConnection);
    }

    @Test
    public void sessionTransacted() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

        JmsProvider.sendMessage(message, session);

        // message was sent but not commited so no message should be delivered back
        checkQueueIsEmpty(jmsConnection);

        session.commit();

        checkQueue(jmsConnection, message);
    }

    /**
     * JMS session chains transactions one after another. When one is commited then another one
     * is started
     */
    @Test
    public void transactionChaining() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

        JmsProvider.sendMessage(message, session);
        Assert.assertTrue("Session should be transacted", session.getTransacted());
        checkQueueIsEmpty(jmsConnection); // not commited yet
        session.commit();
        checkQueue(jmsConnection, message); // already commited
        Assert.assertTrue("Session should be transacted", session.getTransacted());

        // next round of sending
        JmsProvider.sendMessage(message, session);
        checkQueueIsEmpty(jmsConnection); // not commited yet
        session.commit();
        checkQueue(jmsConnection, message); // already commited

        session.close();
        try {
            session.getTransacted();
            Assert.fail("Session should be closed and such throw exception");
        } catch (javax.jms.IllegalStateException e) {
            // this is ok - expecting session being closed
        }
    }

    @Test
    public void transactedWithRollback() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

        JmsProvider.sendMessage(message, session);
        session.commit();

        Assert.assertEquals(message, JmsProvider.receiveMessageAsString(jmsConnection, session));
        session.rollback();

        // session is now rollbacked so no message should be delivered
        JmsProvider.sendMessage(message + "2", session);

        jmsConnection.close();
        jmsConnection = JmsProvider.getConnection();
        session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

        JmsProvider.sendMessage(message + "2", session);

        Assert.assertEquals(message, JmsProvider.receiveMessageAsString(jmsConnection, session));
        try {
            JmsProvider.receiveMessageAsString(jmsConnection, session);
        } catch (NullPointerException npe) {
            // expected behaviour as message + 2 should not be delivered to queue
        }
        session.commit();

        checkQueueIsEmpty(jmsConnection);
    }

    /**
     * Closing session without explicit commit causes that data is not commited - i.e. it's not
     * send to the queue on the remote JMS server.
     * Close means rollback.
     */
    @Test
    public void transactedWithConnectionClosed() throws JMSException {
        log.debug("Sending message {} to through connection {}", message, jmsConnection);
        Session session = jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
        JmsProvider.sendMessage(message, session);

        session.close();

        checkQueueIsEmpty(jmsConnection);
    }

    /**
     * Checking if queue contains expected message or not (new session is created from provided connection).
     */
    private void checkQueue(final Connection conn, final String expectedMessage) throws JMSException {
        TextMessage textMsg = getMessageFromQueue(conn);
        Assert.assertNotNull("There is no message in queue", textMsg);
        Assert.assertEquals("Received message should be the same as sent one", expectedMessage, textMsg.getText());
    }

    /**
     * Verifying that there is no other message in queue
     * Using already created connection
     */
    private void checkQueueIsEmpty(final Connection conn) throws JMSException {
        TextMessage textMessage = getMessageFromQueue(conn);
        Assert.assertNull("Expecting that message was acknowledge and server deleted it from its logs but we received " + textMessage, textMessage);
    }

    private TextMessage getMessageFromQueue(final Connection conn) throws JMSException {
        Session session = null;
        try {
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            return JmsProvider.receiveMessage(conn, session);
        } catch (NullPointerException npe) {
            // there is no message on the jms server
            return null;
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }
}
