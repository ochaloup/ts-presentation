package org.jboss.qa.tspresentation.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.sql.DataSource;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Stateless
public class BeanToCheckEnlistment {
    private static final Logger log = LoggerFactory.getLogger(BeanToCheckEnlistment.class);

    @Resource(lookup = ProjectProperties.NON_XA_DATASOURCE_JNDI)
    DataSource datasource;

    @Resource(lookup = ProjectProperties.NON_XA_NON_JTA_DATASOURCE_JNDI)
    DataSource nonJtaDatasource;

    @Resource(name = "java:/ConnectionFactory")
    private QueueConnectionFactory qcf;

    @Resource(name = ProjectProperties.JMS_QUEUE_JNDI)
    private Queue queue;

    private Random random = new Random();

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void notSupportedDatabase() {
        log.info("Inserting data to DB under not supported");
        insertData();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void notSupportedJms() {
        log.info("Sending message under not supported");
        sendMessage();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void requiresNewDatabase() {
        log.info("Inserting data to DB under requires new");
        insertData();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void requiresNewJms() {
        log.info("Sending message under requires new");
        sendMessage();
    }

    private void insertData() {
        try(Connection c = datasource.getConnection()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO " + JBossTestEntity.TABLE_NAME + " VALUES (?,?)");
            ps.setInt(1, random.nextInt());
            ps.setString(2, "JBoss QE");
            int executeResult = ps.executeUpdate();
            log.info("Insert query executed with int result {}", executeResult);
        } catch (SQLException sqle) {
            log.error("There is some troubles to execute insert query {}", sqle);
            throw new RuntimeException(sqle);
        }
    }

    private void sendMessage() {
        try (javax.jms.Connection connection = qcf.createConnection()) {
            String message = "enlist-message";
            // Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Session session = connection.createSession();
            MessageProducer producer = session.createProducer(queue);
            log.info("Sending message {} to queue {}", message, queue);
            producer.send(session.createTextMessage(message));
            // session.commit();
        } catch (Exception e) {
            log.error("Error in sending a message", e);
            throw new RuntimeException(e);
        }
    }
}
