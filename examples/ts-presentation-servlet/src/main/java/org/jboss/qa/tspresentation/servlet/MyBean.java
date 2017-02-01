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

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MyBean {
    private static final String TABLE = "test";

    @Resource(lookup = "java:jboss/testxa")
    private DataSource dsxa;
    
    @Resource(lookup = "java:jboss/test-jta")
    private DataSource dsjta;
    
    @Resource(lookup = "java:jboss/test-non-jta")
    private DataSource dsnonjta;
 
    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory cf;

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory xacf;
    
    @Resource(mappedName = "java:/jms/queue/test")
    private Queue queueExample;

    @Resource(lookup = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Resource
    private EJBContext context;

    /**
     * <p>
     * Expecting existence of database table TEST
     * <p>
     * <code>CREATE TABLE TEST (id INT, a VARCHAR(255))</code>
     * <p>
     * The purpose of this bean call method is shown what happens when global transaction is started
     * and there is some non-xa resources plus some non-xa non-jta resources (datasources).<br>
     * Transaction is marked to rollback even before 2PC starts.<br>
     * First non-xa datasource is enlisted to transaction as LRCO resource. The second non-xa resource
     * is neither being enlisted to transaction and is just committed as it is.
     * <p>
     * As result of the test we can expect having non-xa+non-jta datasource committed
     * and non-xa connection factory message processed.
     */
    public void call() {
    	System.out.println("Bean " + this.getClass().getName() + " was called");

    	DatabaseUtil.doInsert(dsxa, TABLE, 1, "xa-datasource");
    	DatabaseUtil.doInsert(dsjta, TABLE, 2, "non-xa-datasource but jta");
    	DatabaseUtil.doInsert(dsnonjta, TABLE, 3, "non-xa-datasource and no jta");

    	JMSUtil.sendMessage(cf, queueExample, "connection factory");
    	JMSUtil.sendMessage(xacf, queueExample, "xa connection factory");

    	context.setRollbackOnly();
    }
}
