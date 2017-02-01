package org.jboss.qa.tspresentation.ejb;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.jboss.qa.tspresentation.utils.ResultsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MessageDriven(name = "mdb", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = ProjectProperties.JMS_QUEUE_JNDI)
})
public class BeanToCheckMessageDriven implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(BeanToCheckMessageDriven.class);

    @EJB
    private ResultsBean results;

    @Resource
    private MessageDrivenContext context;

    public void onMessage(final Message message) {
        try {
            String text = ((TextMessage) message).getText();
            log.info("Message '{}' received", text);
            results.addStorageValue("mdb", text);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
