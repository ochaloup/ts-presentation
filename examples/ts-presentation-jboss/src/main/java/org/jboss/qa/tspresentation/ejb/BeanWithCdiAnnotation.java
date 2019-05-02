package org.jboss.qa.tspresentation.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.jboss.qa.tspresentation.cdi.ExceptionWorkerCdiBean;
import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class BeanWithCdiAnnotation {
    private static final Logger log = LoggerFactory.getLogger(BeanWithCdiAnnotation.class);

    @Resource(lookup = ProjectProperties.XA_DATASOURCE_JNDI)
    DataSource datasource;

    @Resource(lookup = "java:/TransactionManager")
    TransactionManager tm;

    @Inject
    ExceptionWorkerCdiBean cdi;

    public void transactionNotSupported(int id, String value) {
    	try {
			System.out.println(">>>> " + tm.getTransaction().toString());
		} catch (SystemException e1) {
			e1.printStackTrace();
		}
    	cdi.doNotRollback();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void transactionRequiresNew(int id, String value) {
        insert(id, value);
    }

    private void insert(int id, String value) {
        String query = "INSERT INTO " + JBossTestEntity.TABLE_NAME + " VALUES (?,?)";
        try(Connection c = datasource.getConnection()) {
            PreparedStatement ps = c.prepareStatement(query);
            ps.setInt(1, id);
            ps.setString(2, value);
            int executeResult = ps.executeUpdate();
            log.info("Insert query executed with int result {}", executeResult);
        } catch (SQLException sqle) {
            log.error("There is some troubles to execute insert query {} with data {}, {}",
                query, id, value, sqle);
            throw new IllegalStateException("Can't insert query " + query
                + " with data " + id + ", " + value, sqle);
        }
    }
}
