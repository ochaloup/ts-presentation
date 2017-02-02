package org.jboss.qa.tspresentation.servlet;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * <p>
 * This bean shows multiple resources which are (or aren't) part of global (XA) transaction.<br>
 * Please check <code>src/main/resources/standalone-full.xml</code> where definitions of particular
 * resources are defined under activemq and datasources subsystems.
 * <p>
 * We have following resources defined here
 * <ul>
 *   <li><b>java:jboss/test-non-jta</b> : standard datasource defined with <i>jta="false"</i>,
 *     it won't be enlisted to global transaction</li>
 *   <li><b>java:jboss/test-jta</b> : standard datasource defined with <i>jta="true"</i>,
 *     it will be enlisted to global transaction and will be used with LRCO</li>
 *   <li><b>java:jboss/testxa</b> : xa datasource,
 *     it will be enlisted to global transaction and it will participate in 2PC protocol handling</li>
 *   <li><b>java:/ConnectionFactory</b> : connection factory,
 *     it won't be enlisted to global transaction (the same as datasource which is non-jta)</li>
 *   <li><b>java:/JmsNonXA</b> : pooled connection factory with <i>transaction="local"</i>,
 *     it will be enlisted to global transaction and will be used with LRCO</li>
 *   <li><b>java:/JmsXA</b> : pooled connection factory with <i>transaction="xa"</i>,
 *     it will be enlisted to global transaction and it will participate in 2PC protocol handling</li>
 * </ul>
 *
 * <p>
 * <b>Disclainer:</b> transaction manager does not allow multiple non-xa resources being added to a global
 * transaction. It's non safe and TM can't guarantee much. If you need override this behavior use system
 * property <code>com.arjuna.ats.arjuna.allowMultipleLastResources</code> being set to <code>true</code>.
 *
 * <p>
 * As result of the call of the bean we expect that all xa and jta capable resources will be rolled back
 * and non-jta resources (connection factory and non-jta datasource) will be committed. Those are not enlisted
 * to global transaction and are not influenced by the fact that the transaction is rolled-back.
 * <p>
 * This test expect to have database running where table <code>TEST</code> exits.<br>
 * <code>CREATE TABLE TEST (id INT, a VARCHAR(255))</code>
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MyBean {
    private static final String TABLE = "test";

    @Resource(lookup = "java:jboss/test-non-jta")
    private DataSource dsnonjta;

    @Resource(lookup = "java:jboss/test-jta")
    private DataSource dsjta;
    
    @Resource(lookup = "java:jboss/testxa")
    private DataSource dsxa;
 
    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory cf;

    @Resource(mappedName = "java:/JmsNonXA")
    private ConnectionFactory nonxacf;

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory xacf;
    
    @Resource(mappedName = "java:/jms/queue/test")
    private Queue queueExample;

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Resource
    private EJBContext context;

    public void call() {
    	System.out.println("Bean " + this.getClass().getName() + " was called");

    	DatabaseUtil.doInsert(dsxa, TABLE, 1, "xa-datasource");
    	DatabaseUtil.doInsert(dsjta, TABLE, 2, "non-xa-datasource but jta");
    	DatabaseUtil.doInsert(dsnonjta, TABLE, 3, "non-xa-datasource and no jta");

    	JMSUtil.sendMessage(cf, queueExample, "connection factory");
    	JMSUtil.sendMessage(nonxacf, queueExample, "non-xa connection factory");
    	JMSUtil.sendMessage(xacf, queueExample, "xa connection factory");

    	context.setRollbackOnly();
    }
}
