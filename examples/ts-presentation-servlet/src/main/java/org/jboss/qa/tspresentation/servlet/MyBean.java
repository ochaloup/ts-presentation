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
     */
    public void call() {
    	System.out.println("Bean " + this.getClass().getName() + " was called");

    	DatabaseUtil.doInsert(dsxa, TABLE, 1, "dsxa");
    	DatabaseUtil.doInsert(dsjta, TABLE, 2, "dsjta");
    	DatabaseUtil.doInsert(dsnonjta, TABLE, 3, "dsjta");

    	JMSUtil.sendMessage(cf, queueExample, "connection factory");
    	JMSUtil.sendMessage(xacf, queueExample, "xa connection factory");

    	context.setRollbackOnly();
    }
}
