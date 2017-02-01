package org.jboss.qa.tspresentation.ejb.failconnection;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.jboss.qa.tspresentation.jpa.JBossTestEntity;
import org.jboss.qa.tspresentation.utils.ProjectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class TableCreator {
    private static final Logger log = LoggerFactory.getLogger(TableCreator.class);

    @Resource(lookup = ProjectProperties.NON_XA_DATASOURCE_JNDI)
    DataSource datasource;

    private static final String CREATE_TABLE_SQL = String.format("CREATE TABLE %s (%s INTEGER NOT NULL UNIQUE, %s VARCHAR(255))",
            JBossTestEntity.TABLE_NAME, "id", JBossTestEntity.NAME_COLUMN_NAME);

    public void createTableWithRethrow() {
        try {
            executeUpdate(CREATE_TABLE_SQL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createTableAndCatch() {
        try {
            executeUpdate(CREATE_TABLE_SQL);
        } catch (Exception e) {
            // ignore
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createTableInNewTxn() {
        try {
            executeUpdate(CREATE_TABLE_SQL);
        } catch (Exception e) {
            // ignore
        }
    }


    private int executeUpdate(final String sql) throws SQLException {
        log.debug("Runnig sql update '{}'", sql);

        try(Connection c = datasource.getConnection()) {
            return c.createStatement().executeUpdate(sql);
        }
    }
}
