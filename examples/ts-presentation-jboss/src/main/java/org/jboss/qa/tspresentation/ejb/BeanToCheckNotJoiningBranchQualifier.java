package org.jboss.qa.tspresentation.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class BeanToCheckNotJoiningBranchQualifier {
    private static final Logger log = LoggerFactory.getLogger(BeanToCheckNotJoiningBranchQualifier.class);

    @Resource(lookup = ProjectProperties.XA_DATASOURCE_JNDI)
    private DataSource datasource;

    @Resource(lookup = ProjectProperties.XA_DATASOURCE_2_JNDI)
    private DataSource datasource2;

    @EJB
    private BeanToCheckNotJoiningBranchQualifier thisBean;

    public static final String TABLE_NAME = "attempt";
    private final Random random = new Random();

    public void runInsertion() {
        log.info("Inserting data to DB under with datasources {} and {}",
                ProjectProperties.XA_DATASOURCE_JNDI, ProjectProperties.XA_DATASOURCE_2_JNDI);

        insertData(TABLE_NAME, datasource);
        insertData(TABLE_NAME, datasource2);
    }

    // TODO: this somelike does not work - could this be an issue of TM or JCA?
    public void runInsertionWithCreation() {
        log.info("Inserting data to DB under with datasources {} and {}",
                ProjectProperties.XA_DATASOURCE_JNDI, ProjectProperties.XA_DATASOURCE_2_JNDI);
        thisBean.createTable(TABLE_NAME);

        log.info("Insertion start...");
        insertData(TABLE_NAME, datasource);
        insertData(TABLE_NAME, datasource2);
    }

    private void insertData(final String tableName, final DataSource ds) {
        try(Connection c = ds.getConnection()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO " + tableName + " VALUES (?)");
            ps.setInt(1, random.nextInt());
            int executeResult = ps.executeUpdate();
            log.info("Insert query executed with int result {}", executeResult);
        } catch (SQLException sqle) {
            log.error("There is some troubles to execute insert query {}", sqle);
            throw new RuntimeException(sqle);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createTable(final String name) {
        try(Connection c = datasource.getConnection()) {
            log.info("Creating table '{}' for datasource ''", name, ProjectProperties.XA_DATASOURCE_JNDI);
            c.createStatement().execute("CREATE TABLE " + name + "(id INTEGER)");
        } catch (Exception sqle) {
            log.info("Can't create table '{}'. Reason: {}", name, sqle.getMessage());
        }
        try(Connection c = datasource2.getConnection()) {
            log.info("Creating table '{}' for datasource ''", name, ProjectProperties.XA_DATASOURCE_2_JNDI);
            c.createStatement().execute("CREATE TABLE " + name + "(id INTEGER)");
        } catch (Exception sqle) {
            log.info("Can't create table '{}'. Reason: {}", name, sqle.getMessage());
        }
    }
}

