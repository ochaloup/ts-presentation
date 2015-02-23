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
 * Using HornetQ native API to send messages
 *
 * For another way how to do it check:
 *  http://java.dzone.com/articles/hornetq-getting-started
 */
public class JMSProvider {
    private static final String HOST = ProjectProperties.get(JMS_HOST);
    private static final String PORT = ProjectProperties.get(JMS_PORT);
    private static final String QUEUE_NAME = ProjectProperties.get(JMS_QUEUE);
    private static final String USERNAME = ProjectProperties.get(JMS_USERNAME);
    private static final String PASSWORD = ProjectProperties.get(JMS_PASSWORD);

    public static void sendMessage(final String text, final Session session) throws JMSException {
        Queue testQueue = session.createQueue(QUEUE_NAME);

        // for sending message I do not need to start connection
        MessageProducer producer = session.createProducer(testQueue);
        TextMessage msg = session.createTextMessage(text);
        producer.send(msg);
    }

    public static String receiveMessage(final Connection connection, final Session session) throws JMSException {
        Queue testQueue = session.createQueue(QUEUE_NAME);

        // receive needs connection being started for consumer to start consume
        // consumer waits till connection is started to start reading messages
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
        if(USERNAME != null && PASSWORD != null ) {
            return cf.createConnection(USERNAME, PASSWORD);
        } else {
            return cf.createConnection();
        }
    }

}
