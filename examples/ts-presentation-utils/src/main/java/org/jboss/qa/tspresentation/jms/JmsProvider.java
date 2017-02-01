package org.jboss.qa.tspresentation.jms;

import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_HOST;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_PASSWORD;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_PORT;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_QUEUE;
import static org.jboss.qa.tspresentation.utils.ProjectProperties.JMS_USERNAME;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Using ActiveMQ Artemis native API to send messages
 * <p>
 * This uses the same way as I did in HornetQ stuff
 * <p>
 * For another way how to do it check:
 *  http://java.dzone.com/articles/hornetq-getting-started
 */
public class JmsProvider {
    private static final Logger log = LoggerFactory.getLogger(JmsProvider.class);

    private static final long RECEIVE_CALL_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
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

    public static TextMessage receiveMessage(final Connection connection, final Session session) throws JMSException {
        Queue testQueue = session.createQueue(QUEUE_NAME);

        // receive needs connection being started for consumer to start consume
        // consumer waits till connection is started to start reading messages
        connection.start();
        MessageConsumer consumer = session.createConsumer(testQueue);
        TextMessage received = (TextMessage) consumer.receive(RECEIVE_CALL_TIMEOUT);
        connection.stop();

        return received;
    }

    public static String receiveMessageAsString(final Connection connection, final Session session) throws JMSException {
        return receiveMessage(connection, session).getText();
    }

    public static Connection getConnection() throws JMSException {
        /*
         * How things were working with HornetQ
         *
         * Map<String, Object> props = new HashMap<String, Object>();
         * props.put(TransportConstants.HOST_PROP_NAME, HOST);
         * props.put(TransportConstants.PORT_PROP_NAME, PORT);
         * props.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
         * props.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
         * TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getCanonicalName(), props);
         *
         * HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);
         * cf.setCallTimeout(RECEIVE_CALL_TIMEOUT);
         */

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TransportConstants.HOST_PROP_NAME, HOST);
        props.put(TransportConstants.PORT_PROP_NAME, PORT);
        // this is needed for WildFly as http protocol upgrade has to be enabled
        props.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        props.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getCanonicalName(), props);

        ActiveMQConnectionFactory cf = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);
        cf.setCallTimeout(RECEIVE_CALL_TIMEOUT);

        if(USERNAME != null && PASSWORD != null && !USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            log.debug("Creating JMS connection to {}:{} for user '{}' and password '{}'", HOST, PORT, USERNAME, PASSWORD);
            try {
                return cf.createConnection(USERNAME, PASSWORD);
            } catch(JMSException jmse) {
                String errorMsg = String.format("Can't create a connection to %s:%s with credentials %s/%s",
                    HOST, PORT, USERNAME, PASSWORD);
                log.error(errorMsg, jmse);
                throw new IllegalStateException(errorMsg, jmse);
            }
        } else {
            log.debug("Creating JMS connection to {}:{} without authentication", HOST, PORT);
            try {
                return cf.createConnection();
            } catch(JMSException jmse) {
                String errorMsg = String.format("Can't create a connection to %s:%s with no credentials", HOST, PORT);
                log.error(errorMsg, jmse);
                throw new IllegalStateException(errorMsg, jmse);
            }
        }
    }

}
