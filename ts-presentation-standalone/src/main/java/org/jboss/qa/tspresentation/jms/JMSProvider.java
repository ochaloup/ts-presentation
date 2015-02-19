package org.jboss.qa.tspresentation.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.*;

/**
 * Using HornetQ native API
 */
public class JMSProvider {
    private static final String HOST = ProjectProperties.get(JMS_HOST);
    private static final String PORT = ProjectProperties.get(JMS_PORT);
    private static final String QUEUE_NAME = ProjectProperties.get(JMS_QUEUE);

    public static void sendMessage(final String text, final Session session) throws JMSException {
        Queue testQueue = session.createQueue(QUEUE_NAME);

        MessageProducer producer = session.createProducer(testQueue);
        TextMessage msg = session.createTextMessage(text);
        producer.send(msg);
    }

    public static String receiveMessage(final Connection connection, final Session session) throws JMSException {
        Queue testQueue = session.createQueue(QUEUE_NAME);

        connection.start();
        MessageConsumer consumer = session.createConsumer(testQueue);
        TextMessage received = (TextMessage) consumer.receive(10000L);
        connection.stop();

        return received.getText();
    }

    public static Connection getConnection() throws JMSException {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TransportConstants.HOST_PROP_NAME, HOST);
        props.put(TransportConstants.PORT_PROP_NAME, PORT);
        TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getCanonicalName(), props);

        HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);
        cf.setCallTimeout(3000L);
        return cf.createConnection("guest", "guest");
        // return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    /*
     * http://java.dzone.com/articles/hornetq-getting-started
    public void sendTextMessage2(final String text) {
        // Step 1. Create an initial context to perform the JNDI lookup.
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.PROVIDER_URL, "jnp://localhost:1099");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces  ");
        Context ctx = new InitialContext(env);

        // Step 2. Lookup the connection factory
        ConnectionFactory cf = (ConnectionFactory)ctx.lookup("/ConnectionFactory");

        // Step 3. Lookup the JMS queue
        Queue queue = (Queue)ctx.lookup("/queue/ExampleQueue");

        // Step 4. Create the JMS objects to connect to the server and manage a session
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Step 5. Create a JMS Message Producer to send a message on the queue
        MessageProducer producer = session.createProducer(queue);

        // Step 6. Create a Text Message and send it using the producer
        TextMessage message = session.createTextMessage("Hello, HornetQ!");
        producer.send(message);
        System.out.println("Sent message: " + message.getText());

        // now that the message has been sent, let's receive it

        // Step 7. Create a JMS Message Consumer to receive message from the queue
        MessageConsumer messageConsumer = session.createConsumer(queue);

        // Step 8. Start the Connection so that the server starts to deliver messages
        connection.start();

        // Step 9. Receive the message
        TextMessage messageReceived = (TextMessage)messageConsumer.receive(5000);
        System.out.println("Received message: " + messageReceived.getText());

        // Finally, we clean up all the JMS resources
        connection.close();
        // Step 1. Create an initial context to perform the JNDI lookup.
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.PROVIDER_URL, "jnp://localhost:1099");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces  ");
        Context ctx = new InitialContext(env);

        // Step 2. Lookup the connection factory
        ConnectionFactory cf = (ConnectionFactory)ctx.lookup("/ConnectionFactory");

        // Step 3. Lookup the JMS queue
        Queue queue = (Queue)ctx.lookup("/queue/ExampleQueue");

        // Step 4. Create the JMS objects to connect to the server and manage a session
        Connection connection = cf.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Step 5. Create a JMS Message Producer to send a message on the queue
        MessageProducer producer = session.createProducer(queue);

        // Step 6. Create a Text Message and send it using the producer
        TextMessage message = session.createTextMessage("Hello, HornetQ!");
        producer.send(message);
        System.out.println("Sent message: " + message.getText());

        // now that the message has been sent, let's receive it

        // Step 7. Create a JMS Message Consumer to receive message from the queue
        MessageConsumer messageConsumer = session.createConsumer(queue);

        // Step 8. Start the Connection so that the server starts to deliver messages
        connection.start();

        // Step 9. Receive the message
        TextMessage messageReceived = (TextMessage)messageConsumer.receive(5000);
        System.out.println("Received message: " + messageReceived.getText());

        // Finally, we clean up all the JMS resources
        connection.close();
    }
    */
}
