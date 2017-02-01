package org.jboss.qa.tspresentation.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class JdbcBean {
    private static final Logger log = LoggerFactory.getLogger(JdbcBean.class);

    @Resource(lookup = ProjectProperties.NON_XA_DATASOURCE_JNDI)
    DataSource datasource;

    public List<String[]> query(final String sql) throws SQLException {
        log.debug("Runnig sql query '{}'", sql);

        try(Connection c = datasource.getConnection()) {
            ResultSet rs = c.createStatement().executeQuery(sql);
            int columnCount = rs.getMetaData().getColumnCount();
            List<String[]> table = new ArrayList<>();
            while(rs.next()) {
                String[] row = new String[columnCount];
                for( int currentColumn = 1; currentColumn <= columnCount; currentColumn++ ){
                    row[currentColumn-1] = rs.getObject(currentColumn).toString();
                }
                table.add( row );
            }
            return table;
        }
    }

    /**
     * Looking for id value in column 'id' from table tableName and returning first row
     * of results from the columnNameToReturn
     */
    public String getSingle(final String tableName, final Integer idValue, final String columnNameToReturn) throws SQLException {
        try(Connection c = datasource.getConnection()) {
            PreparedStatement ps = c.prepareStatement("SELECT " + columnNameToReturn + " FROM " + tableName + " WHERE id = ?");
            ps.setInt(1, idValue);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getObject(1).toString();
            } else {
                return null;
            }
        }
    }
}
