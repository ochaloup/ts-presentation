package org.jboss.qa.tspresentation.servlet;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class JMSUtil {
	private JMSUtil() {
		// util class
	}

    public static void sendMessage(ConnectionFactory connectionFactory, Destination queue, String message) {
        javax.jms.Connection connection = null;
        try {         
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer publisher = null;
 
            publisher = session.createProducer(queue);
 
            connection.start();
 
            TextMessage textMessage = session.createTextMessage(message);
            publisher.send(textMessage);
            System.out.println("mesage '" + textMessage + "' was sent");
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        finally {         
            if (connection != null)   {
                try {
                    connection.close();
                } catch (JMSException e) {                    
                    e.printStackTrace();
                }
 
            }
        }
    }
}
